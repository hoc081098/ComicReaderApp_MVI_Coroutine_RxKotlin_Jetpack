package com.hoc.comicapp.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.*
import androidx.work.WorkInfo.State.*
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.detail.ComicDetailViewState.Chapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.*
import com.hoc.comicapp.utils.combineLatest
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.notOfType
import com.hoc.comicapp.worker.DownloadComicWorker
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.shopify.livedataktx.MutableLiveDataKtx
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxSingle
import timber.log.Timber
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
class ComicDetailViewModel(
  private val comicDetailInteractor: ComicDetailInteractor,
  private val workManager: WorkManager,
  private val downloadComicsRepository: DownloadComicsRepository,
  private val rxSchedulerProvider: RxSchedulerProvider
) :
  BaseViewModel<ComicDetailIntent, ComicDetailViewState, ComicDetailSingleEvent>(), Observer<ComicDetailViewState> {
  override fun onChanged(t: ComicDetailViewState?) {
    setNewState(t ?: return)
  }

  private val _stateD: LiveData<ComicDetailViewState>
  override val initialState = ComicDetailViewState.initialState()

  private val intentS = PublishRelay.create<ComicDetailIntent>()
  private val stateS = BehaviorRelay.createDefault<ComicDetailViewState>(initialState)

  override fun processIntents(intents: Observable<ComicDetailIntent>) =
    intents.subscribe(intentS)!!

  private val initialProcessor =
    ObservableTransformer<ComicDetailIntent.Initial, ComicDetailPartialChange> { intent ->
      intent.flatMap {
        comicDetailInteractor.getComicDetail(
          it.link,
          it.title,
          it.thumbnail
        ).doOnNext {
          val message =
            (it as? ComicDetailPartialChange.InitialRetryPartialChange.Error ?: return@doOnNext)
              .error
              .getMessage()
          sendMessageEvent("Get detail comic error: $message")
        }
      }
    }

  private val retryProcessor =
    ObservableTransformer<ComicDetailIntent.Retry, ComicDetailPartialChange> { intent ->
      intent.flatMap {
        comicDetailInteractor.getComicDetail(it.link)
          .doOnNext {
            val message =
              (it as? ComicDetailPartialChange.InitialRetryPartialChange.Error ?: return@doOnNext)
                .error
                .getMessage()
            sendMessageEvent("Retry get detail comic error: $message")
          }
      }
    }

  private val refreshProcessor =
    ObservableTransformer<ComicDetailIntent.Refresh, ComicDetailPartialChange> { intentObservable ->
      intentObservable
        .exhaustMap { intent ->
          comicDetailInteractor
            .refreshPartialChanges(intent.link)
            .doOnNext {
              sendMessageEvent(
                when (it) {
                  is ComicDetailPartialChange.RefreshPartialChange.Success -> "Refresh successfully"
                  is ComicDetailPartialChange.RefreshPartialChange.Error -> "Refresh not successfully, error: ${it.error.getMessage()}"
                  else -> return@doOnNext
                }
              )
            }
        }
    }

  private val intentToViewState = ObservableTransformer<ComicDetailIntent, ComicDetailViewState> {
    it.publish { shared ->
      Observable.mergeArray(
        shared.ofType<ComicDetailIntent.Initial>().compose(initialProcessor),
        shared.ofType<ComicDetailIntent.Refresh>().compose(refreshProcessor),
        shared.ofType<ComicDetailIntent.Retry>().compose(retryProcessor)
      )
    }.doOnNext { Timber.d("partial_change=$it") }
      .scan(initialState) { state, change -> change.reducer(state) }
      .distinctUntilChanged()
      .observeOn(rxSchedulerProvider.main)
  }

  init {
    val filteredIntent = intentS
      .compose(intentFilter)
      .doOnNext { Timber.d("intent=$it") }

    _stateD = initStateD(filteredIntent)
    processDownloadChapterIntent(filteredIntent)
    processDeleteAndCancelDownloadingChapterIntent(filteredIntent)
  }

  private fun initStateD(filteredIntent: Observable<ComicDetailIntent>): LiveData<ComicDetailViewState> {
    // intent -> behavior subject
    filteredIntent
      .compose(intentToViewState)
      .doOnNext { Timber.d("view_state=$it") }
      .subscribeBy(onNext = stateS::accept)
      .addTo(compositeDisposable)

    // behavior subject -> live data
    val stateD = MutableLiveDataKtx<ComicDetailViewState>().apply { value = initialState }
    stateS
      .subscribeBy(onNext = stateD::setValue)
      .addTo(compositeDisposable)

    // combine live datas -> state live data
    val workInfosD = workManager.getWorkInfosByTagLiveData(DownloadComicWorker.TAG)
    val downloadedChaptersD = downloadComicsRepository.downloadedChapters()

    return stateD.combineLatest(workInfosD, downloadedChaptersD) { state, workInfos, downloadedChapters ->
      Timber.d("[combine] ${workInfos.size} ${downloadedChapters.size}")

      val comicDetail = state.comicDetail as? ComicDetailViewState.ComicDetail.Detail
        ?: return@combineLatest state

      val newChapters = comicDetail.chapters.map { chapter ->
        chapter.copy(
          downloadState = getDownloadState(
            workInfos,
            chapter,
            downloadedChapters
          )
        )
      }

      state.copy(comicDetail = comicDetail.copy(chapters = newChapters))
    }.apply { observeForever(this@ComicDetailViewModel) }
  }

  private fun processDeleteAndCancelDownloadingChapterIntent(filteredIntent: Observable<ComicDetailIntent>) {
    Observable
      .merge(
        filteredIntent
          .ofType<ComicDetailIntent.DeleteChapter>()
          .map { it.chapter to true },
        filteredIntent
          .ofType<ComicDetailIntent.CancelDownloadChapter>()
          .map { it.chapter to false }
      )
      .flatMap { (chapter, delete) ->
        deleteDownloadedChapter(chapter)
          .toObservable()
          .map { Triple(it.first, it.second, delete) }
          .onErrorReturn { Triple(chapter, null, delete) }
      }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy {
        val operation = if (it.third) "Delete download" else "Cancel download"
        when {
          it.second === null -> {
            Timber.d("$operation success $it")
            sendMessageEvent("$operation ${it.first.chapterName}")
          }
          else -> {
            Timber.d("$operation error $it")
            sendMessageEvent("$operation error: ${it.first.chapterName}")
          }
        }
      }
      .addTo(compositeDisposable)
  }

  override fun onCleared() {
    super.onCleared()
    _stateD.removeObserver(this)
  }

  private fun processDownloadChapterIntent(filteredIntent: Observable<ComicDetailIntent>) {
    filteredIntent
      .ofType<ComicDetailIntent.DownloadChapter>()
      .map { it.chapter }
      .flatMap { chapter ->
        enqueueDownloadComicWorker(chapter)
          .toObservable()
          .onErrorReturn { chapter to it }
      }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy {
        when {
          it.second === null -> {
            Timber.d("Enqueue success $it")
            sendMessageEvent("Enqueued download ${it.first.chapterName}")
          }
          else -> {
            Timber.d("Enqueue error $it")
            sendMessageEvent("Enqueued error: ${it.first.chapterName}")
          }
        }
      }
      .addTo(compositeDisposable)
  }

  private fun getDownloadState(
    workInfos: List<WorkInfo>,
    chapter: Chapter,
    downloadedChapters: List<DownloadedChapter>
  ): DownloadState {
    return when {
      downloadedChapters.any { it.chapterLink == chapter.chapterLink } -> Downloaded
      else -> when (
        val workInfo = workInfos.find { chapter.chapterLink in it.tags && it.state == RUNNING }) {
        null -> NotYetDownload
        else -> Downloading(
          workInfo.progress.getInt(
            DownloadComicWorker.PROGRESS,
            0
          )
        ).also {
          Timber.tag("####").d(workInfo.toString())
          Timber.tag("####").d(it.toString())
        }
      }
    }
  }

  private fun sendMessageEvent(message: String) {
    sendEvent(ComicDetailSingleEvent.MessageEvent(message))
  }

  private companion object {
    private val intentFilter = ObservableTransformer<ComicDetailIntent, ComicDetailIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared.ofType<ComicDetailIntent.Initial>().take(1),
          shared.notOfType<ComicDetailIntent.Initial, ComicDetailIntent>()
        )
      }
    }
  }

  private fun enqueueDownloadComicWorker(chapter: Chapter): Single<Pair<Chapter, Throwable?>> {
    return rxSingle {
      workManager.cancelAllWorkByTag(chapter.chapterLink).await()
      val workRequest = OneTimeWorkRequestBuilder<DownloadComicWorker>()
        .setInputData(
          workDataOf(
            DownloadComicWorker.CHAPTER_LINK to chapter.chapterLink,
            DownloadComicWorker.CHAPTER_NAME to chapter.chapterName,
            DownloadComicWorker.COMIC_NAME to when (val detail = state.value.comicDetail) {
              is ComicDetailViewState.ComicDetail.Detail -> detail.title
              is ComicDetailViewState.ComicDetail.Initial -> detail.title
              null -> return@rxSingle chapter to IllegalStateException("State is null")
            }
          )
        )
        .setConstraints(
          Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
        )
        .addTag(DownloadComicWorker.TAG)
        .addTag(chapter.chapterLink)
        .build()
      workManager.enqueue(workRequest).await()
      chapter to null
    }
  }

  private fun deleteDownloadedChapter(chapter: Chapter): Single<Pair<Chapter, Throwable?>> {
    return rxSingle {
      workManager.cancelAllWorkByTag(chapter.chapterLink).await()
      downloadComicsRepository
        .deleteDownloadedChapter(chapter = chapter.toDomain())
        .fold(
          { chapter to it },
          { chapter to null }
        )
    }
  }
}