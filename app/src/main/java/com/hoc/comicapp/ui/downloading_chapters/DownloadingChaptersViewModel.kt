package com.hoc.comicapp.ui.downloading_chapters

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.data.JsonAdaptersContainer
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.UnexpectedError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.PartialChange
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.SingleEvent
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewIntent
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState.Chapter
import com.hoc.comicapp.utils.notOfType
import com.hoc.comicapp.utils.toObservable
import com.hoc.comicapp.worker.DownloadComicWorker
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.rx3.rxSingle
import timber.log.Timber

class DownloadingChaptersViewModel(
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val workManager: WorkManager,
  private val jsonAdaptersContainer: JsonAdaptersContainer,
  private val downloadComicsRepository: DownloadComicsRepository,
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>(ViewState.initial()) {

  private val intentS = PublishRelay.create<ViewIntent>()

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intents ->
    Observable.mergeArray(
      intents.ofType<ViewIntent.Initial>().compose(initialProcessor)
    )
  }

  private val initialProcessor =
    ObservableTransformer<ViewIntent.Initial, PartialChange> { intents ->
      intents.flatMap {
        workManager
          .getWorkInfosByTagLiveData(DownloadComicWorker.TAG)
          .toObservable { emptyList() }
          .observeOn(rxSchedulerProvider.io)
          .map { workInfos ->
            workInfos.mapNotNull {
              if (it.state != WorkInfo.State.RUNNING) return@mapNotNull null

              val comicName =
                it.progress.getString(DownloadComicWorker.COMIC_NAME) ?: return@mapNotNull null
              val chapterJson =
                it.progress.getString(DownloadComicWorker.CHAPTER) ?: return@mapNotNull null
              val (chapterLink, chapterName) =
                jsonAdaptersContainer.comicDetailChapterAdapter.fromJson(chapterJson)
                  ?: return@mapNotNull null

              Chapter(
                title = chapterName,
                link = chapterLink,
                progress = it.progress.getInt(DownloadComicWorker.PROGRESS, 0),
                comicTitle = comicName
              )
            }
          }
          .map<PartialChange> { PartialChange.Data(it) }
          .onErrorReturn { t: Throwable -> PartialChange.Error(UnexpectedError("", t)) }
          .observeOn(rxSchedulerProvider.main)
          .startWithItem(PartialChange.Loading)
      }
    }

  override fun processIntents(intents: Observable<ViewIntent>): Disposable =
    intents.subscribe(intentS)

  init {
    Timber.d("DownloadingChaptersViewModel::init")

    val filteredIntent = intentS.compose(intentFilter).share()

    filteredIntent
      .compose(intentToChanges)
      .scan(initialState, reducer)
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = setNewState)
      .addTo(compositeDisposable)

    filteredIntent
      .ofType<ViewIntent.CancelDownload>()
      .map { it.chapter }
      .flatMap(::deleteDownloadingChapter)
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy {
        when (val error = it.second) {
          null -> sendEvent(SingleEvent.Deleted(it.first))
          else -> sendEvent(SingleEvent.DeleteError(it.first, error))
        }
      }
      .addTo(compositeDisposable)
  }

  override fun onCleared() {
    super.onCleared()
    Timber.d("DownloadingChaptersViewModel::onCleared")
  }

  private fun deleteDownloadingChapter(chapter: Chapter): Observable<Pair<Chapter, ComicAppError?>> {
    return rxSingle {
      downloadComicsRepository
        .deleteDownloadedChapter(chapter = chapter.toDomain())
        .fold(
          { chapter to it },
          { chapter to null }
        )
    }
      .toObservable()
      .onErrorReturn { chapter to UnexpectedError(it.message ?: "", it) }
  }

  private companion object {
    val intentFilter = ObservableTransformer<ViewIntent, ViewIntent> { intents ->
      intents.publish { shared ->
        Observable.mergeArray(
          shared.ofType<ViewIntent.Initial>().take(1),
          shared.notOfType<ViewIntent.Initial, ViewIntent>()
        )
      }
    }

    val reducer = BiFunction<ViewState, PartialChange, ViewState> { vs, change ->
      when (change) {
        is PartialChange.Data -> {
          vs.copy(
            isLoading = false,
            error = null,
            chapters = change.chapters
          )
        }
        is PartialChange.Error -> {
          vs.copy(
            isLoading = false,
            error = change.error.getMessage()
          )
        }
        PartialChange.Loading -> {
          vs.copy(isLoading = true)
        }
      }
    }
  }
}
