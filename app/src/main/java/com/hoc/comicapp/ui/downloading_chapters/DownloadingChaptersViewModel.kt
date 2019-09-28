package com.hoc.comicapp.ui.downloading_chapters

import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.UnexpectedError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.*
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState.Chapter
import com.hoc.comicapp.utils.notOfType
import com.hoc.comicapp.utils.toObservable
import com.hoc.comicapp.worker.DownloadComicWorker
import com.jakewharton.rxrelay2.PublishRelay
import com.squareup.moshi.JsonAdapter
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@ExperimentalCoroutinesApi
class DownloadingChaptersViewModel(
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val workManager: WorkManager,
  private val chapterJsonAdapter: JsonAdapter<ComicDetail.Chapter>
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>() {

  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<ViewIntent>()

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intents ->
    Observable.mergeArray(
      intents.ofType<ViewIntent.Initial>().compose(initialProcessor)
    )
  }

  private val initialProcessor = ObservableTransformer<ViewIntent.Initial, PartialChange> { intents ->
    intents.flatMap {
      workManager
        .getWorkInfosByTagLiveData(DownloadComicWorker.TAG)
        .toObservable { emptyList() }
        .observeOn(rxSchedulerProvider.io)
        .map { workInfos ->
          workInfos.mapNotNull {
            if (it.state != WorkInfo.State.RUNNING) return@mapNotNull null

            val comicName = it.progress.getString(DownloadComicWorker.COMIC_NAME) ?: return@mapNotNull null
            val chapterJson = it.progress.getString(DownloadComicWorker.CHAPTER) ?: return@mapNotNull null
            val (chapterLink, chapterName) = chapterJsonAdapter.fromJson(chapterJson)
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
        .startWith(PartialChange.Loading)
    }
  }

  override fun processIntents(intents: Observable<ViewIntent>) = intents.subscribe(intentS)!!

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToChanges)
      .scan(initialState, reducer)
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
    Timber.d("DownloadingChaptersViewModel::init")
  }

  override fun onCleared() {
    super.onCleared()
    Timber.d("DownloadingChaptersViewModel::onCleared")
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