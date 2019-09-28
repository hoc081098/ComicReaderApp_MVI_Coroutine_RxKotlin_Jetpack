package com.hoc.comicapp.ui.downloading_chapters

import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.UnexpectedError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.*
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState.Chapter
import com.hoc.comicapp.worker.DownloadComicWorker
import com.jakewharton.rxrelay2.PublishRelay
import com.squareup.moshi.JsonAdapter
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class DownloadingChaptersViewModel(
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val workManager: WorkManager,
  private val chapterJsonAdapter: JsonAdapter<ComicDetail.Chapter>
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>(), Observer<MutableList<WorkInfo>> {
  override fun onChanged(infos: MutableList<WorkInfo>?) {
    workInfosS.onNext(infos ?: emptyList())
  }

  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<ViewIntent>()

  private val workInfosByTagLiveData = workManager.getWorkInfosByTagLiveData(DownloadComicWorker.TAG)
  private val workInfosS = BehaviorSubject.create<List<WorkInfo>>()

  override fun processIntents(intents: Observable<ViewIntent>) = intents.subscribe(intentS)!!

  init {
    workInfosByTagLiveData.observeForever(this)
    workInfosS
      .observeOn(rxSchedulerProvider.io)
      .map { workInfos ->
        workInfos
          .mapNotNull {
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
      .scan(initialState, reducer)
      .distinctUntilChanged()
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  override fun onCleared() {
    super.onCleared()
    workInfosByTagLiveData.removeObserver(this)
  }

  private companion object {
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