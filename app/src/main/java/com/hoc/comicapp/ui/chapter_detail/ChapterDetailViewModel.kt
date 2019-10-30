package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.Interactor
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.PartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.SingleEvent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewIntent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState.Chapter
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.mapNotNull
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class ChapterDetailViewModel(
  private val interactor: Interactor,
  rxSchedulerProvider: RxSchedulerProvider,
  private val isDownloaded: Boolean
) :
  BaseViewModel<ViewIntent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<ViewIntent>()

  private val currentChapter
    get() = state.value.detail?.chapter ?: error("State is null")

  private val nextChapter: Chapter?
    get() {
      val detail = (state.value.detail as? ViewState.Detail.Data) ?: return null
      return detail.nextChapterLink?.let { nextChapterLink ->
        detail
          .chapters
          .find { it.link == nextChapterLink }
          ?.name
          ?.let { Chapter(name = it, link = nextChapterLink) }
      }
    }

  private val prevChapter: Chapter?
    get() {
      val detail = (state.value.detail as? ViewState.Detail.Data) ?: return null
      return detail.prevChapterLink?.let { prevChapterLink ->
        detail
          .chapters
          .find { it.link == prevChapterLink }
          ?.name
          ?.let { Chapter(name = it, link = prevChapterLink) }
      }
    }

  private val refreshProcessor =
    ObservableTransformer<ViewIntent.Refresh, PartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .refresh(currentChapter, isDownloaded)
          .doOnNext {
            when (it) {
              is PartialChange.RefreshPartialChange.Error -> sendMessageEvent(message = "Refresh error occurred: ${it.error.getMessage()}")
              is PartialChange.RefreshPartialChange.Success -> sendMessageEvent(message = "Refresh success")
            }
          }
      }
    }

  private val retryProcessor =
    ObservableTransformer<ViewIntent.Retry, PartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .getChapterDetail(currentChapter, isDownloaded)
          .doOnNext {
            if (it is PartialChange.InitialRetryLoadChapterPartialChange.Error) {
              sendMessageEvent(message = "Retry error occurred: ${it.error.getMessage()}")
            }
          }
      }
    }

  private val loadChapterProcessor =
    ObservableTransformer<ViewIntent.LoadChapter, PartialChange> { intents ->
      intents
        .switchMap { intent ->
          interactor
            .getChapterDetail(intent.chapter, isDownloaded)
            .doOnNext {
              if (it is PartialChange.InitialRetryLoadChapterPartialChange.Error) {
                sendMessageEvent("Load ${intent.chapter.name} error occurred: ${it.error.getMessage()}")
              }
            }
        }
    }

  private val intentToChanges =
    ObservableTransformer<ViewIntent, PartialChange> { intents ->
      Observable.mergeArray(
        Observable.mergeArray(
          intents.ofType(),
          intents.ofType<ViewIntent.Initial>().map { ViewIntent.LoadChapter(it.chapter) },
          intents.ofType<ViewIntent.LoadNextChapter>()
            .mapNotNull { nextChapter }
            .map { ViewIntent.LoadChapter(it) },
          intents.ofType<ViewIntent.LoadPrevChapter>()
            .mapNotNull { prevChapter }
            .map { ViewIntent.LoadChapter(it) }
        )
          .distinctUntilChanged()
          .compose(loadChapterProcessor),
        intents.ofType<ViewIntent.Refresh>().compose(refreshProcessor),
        intents.ofType<ViewIntent.Retry>().compose(retryProcessor)
      )
    }

  override fun processIntents(intents: Observable<ViewIntent>) =
    intents.subscribe(intentS)!!

  init {
    intentS
      .compose(intentFilter)
      .publish { intents ->
        Observables.combineLatest(
          intents
            .compose(intentToChanges)
            .scan(initialState) { vs, change -> change.reducer(vs) },
          intents
            .ofType<ViewIntent.ChangeOrientation>()
            .map { it.orientation }
        ) { state, orientation -> state.copy(orientation = orientation) }
      }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  /**
   * Send [message]
   */
  private fun sendMessageEvent(message: String) =
    sendEvent(SingleEvent.MessageEvent(message))

  private companion object {
    /**
     * Only take 1 [ViewIntent.Initial]
     */
    @JvmStatic
    private val intentFilter = ObservableTransformer<ViewIntent, ViewIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared
            .ofType<ViewIntent.Initial>()
            .take(1),
          shared.notOfType<ViewIntent.Initial, ViewIntent>()
        )
      }
    }
  }
}