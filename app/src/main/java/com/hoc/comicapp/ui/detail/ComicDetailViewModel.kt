package com.hoc.comicapp.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkManager
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.UnexpectedError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.detail.ComicDetailViewState.Chapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloaded
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloading
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.NotYetDownload
import com.hoc.comicapp.utils.NotNullMutableLiveData
import com.hoc.comicapp.utils.combineLatest
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.mapNotNull
import com.hoc.comicapp.utils.notOfType
import com.hoc.comicapp.worker.DownloadComicWorker
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.withLatestFrom
import timber.log.Timber

class ComicDetailViewModel(
  private val comicDetailInteractor: ComicDetailInteractor,
  private val downloadComicsRepository: DownloadComicsRepository,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val workManager: WorkManager,
  private val isDownloaded: Boolean,
) : BaseViewModel<ComicDetailIntent, ComicDetailViewState, ComicDetailSingleEvent>(
  ComicDetailViewState.initialState()
),
  Observer<ComicDetailViewState> {

  override fun onChanged(t: ComicDetailViewState?) {
    setNewState(t ?: return)
  }

  private val _stateD: LiveData<ComicDetailViewState>

  private val intentS = PublishRelay.create<ComicDetailIntent>()
  private val stateS = BehaviorRelay.createDefault(initialState)

  override fun processIntents(intents: Observable<ComicDetailIntent>): Disposable =
    intents.subscribe(intentS)

  private val initialProcessor =
    ObservableTransformer<ComicDetailIntent.Initial, ComicDetailPartialChange> { intent ->
      intent.flatMap { (comicArg) ->
        comicDetailInteractor
          .getComicDetail(
            link = comicArg.link,
            name = comicArg.title,
            thumbnail = comicArg.thumbnail,
            view = comicArg.view,
            remoteThumbnail = comicArg.remoteThumbnail,
            isDownloaded = isDownloaded
          )
          .mergeWith(comicDetailInteractor.getFavoriteChange(comicArg.link))
          .doOnNext {
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
      intent
        .withLatestFrom(stateS)
        .mapNotNull { it.second.comicDetail?.link }
        .flatMap { link ->
          comicDetailInteractor
            .getComicDetail(link, isDownloaded = isDownloaded)
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
        .withLatestFrom(stateS)
        .mapNotNull { it.second.comicDetail?.link }
        .exhaustMap { link ->
          comicDetailInteractor
            .refreshPartialChanges(link, isDownloaded = isDownloaded)
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

  private val intentToViewState =
    ObservableTransformer<ComicDetailIntent, ComicDetailViewState> { intent ->
      intent.publish { shared ->
        Observable.mergeArray(
          shared.ofType<ComicDetailIntent.Initial>().compose(initialProcessor),
          shared.ofType<ComicDetailIntent.Refresh>().compose(refreshProcessor),
          shared.ofType<ComicDetailIntent.Retry>().compose(retryProcessor)
        )
      }
        .doOnNext { Timber.d("partial_change=$it") }
        .scan(initialState) { state, change -> change.reducer(state) }
        .distinctUntilChanged()
        .observeOn(rxSchedulerProvider.main)
    }

  init {
    val filteredIntent = intentS
      .compose(intentFilter)
      .doOnNext { Timber.d("intent=$it") }
      .share()

    _stateD = initStateD(filteredIntent)
    processDownloadChapterIntent(filteredIntent)
    processDeleteAndCancelDownloadingChapterIntent(filteredIntent)
    processToggleFavorite(filteredIntent)
  }

  private fun processToggleFavorite(filteredIntent: Observable<ComicDetailIntent>) {
    filteredIntent
      .ofType<ComicDetailIntent.ToggleFavorite>()
      .withLatestFrom(stateS)
      .mapNotNull { it.second.comicDetail }
      .concatMap {
        comicDetailInteractor
          .toggleFavorite(it)
          .onErrorReturnItem(Unit)
      }
      .subscribeBy { Timber.d("[TOGGLE_FAV] $it") }
      .addTo(compositeDisposable)
  }

  private fun initStateD(filteredIntent: Observable<ComicDetailIntent>): LiveData<ComicDetailViewState> {
    // intent -> behavior subject
    filteredIntent
      .compose(intentToViewState)
      .doOnNext { Timber.d("view_state=$it") }
      .subscribeBy(onNext = stateS::accept)
      .addTo(compositeDisposable)

    // behavior subject -> live data
    val stateD = NotNullMutableLiveData(initialState)
    stateS
      .subscribeBy(onNext = stateD::setValue)
      .addTo(compositeDisposable)

    // combine live datas -> state live data
    val workInfosD = workManager.getWorkInfosByTagLiveData(DownloadComicWorker.TAG)
    val downloadedChaptersD = downloadComicsRepository.getDownloadedChapters()

    return stateD.combineLatest(
      workInfosD,
      downloadedChaptersD
    ) { state, workInfos, downloadedChapters ->
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
        comicDetailInteractor
          .deleteOrCancelDownload(chapter)
          .map { it to delete }
      }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy {
        val operation = if (it.second) "Delete download" else "Cancel download"
        when (val event = it.first) {
          is ComicDetailSingleEvent.DeletedChapter -> {
            Timber.d("$operation success $it")
            sendMessageEvent("$operation ${event.chapter.chapterName}")
          }
          is ComicDetailSingleEvent.DeleteChapterError -> {
            Timber.d("$operation error $it")
            sendMessageEvent("$operation error: ${event.chapter.chapterName}")
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
        Observable.defer {
          val comicName = when (val detail = state.value.comicDetail) {
            is ComicDetailViewState.ComicDetail.Detail -> detail.title
            is ComicDetailViewState.ComicDetail.Initial -> detail.title
            null -> return@defer Observable.just(
              ComicDetailSingleEvent.EnqueuedDownloadFailure(
                chapter,
                UnexpectedError(
                  "State is null",
                  IllegalStateException("State is null")
                )
              )
            )
          }
          comicDetailInteractor.enqueueDownloadComic(chapter, comicName)
        }
      }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy { event ->
        Timber.d("Enqueue result $event")
        sendEvent(event)
      }
      .addTo(compositeDisposable)
  }

  private fun getDownloadState(
    workInfos: List<WorkInfo>,
    chapter: Chapter,
    downloadedChapters: List<DownloadedChapter>,
  ): DownloadState {
    return when {
      downloadedChapters.any { it.chapterLink == chapter.chapterLink } -> Downloaded
      else ->
        when (
          val workInfo = workInfos.find { chapter.chapterLink in it.tags && it.state == RUNNING }
        ) {
          null -> NotYetDownload
          else ->
            Downloading(
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
}
