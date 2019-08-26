package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.Some
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.hoc.comicapp.utils.toOptional
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class ChapterDetailViewModel(
  private val interactor: ChapterDetailInteractor,
  rxSchedulerProvider: RxSchedulerProvider
) :
  BaseViewModel<ChapterDetailViewIntent, ChapterDetailViewState, ChapterDetailSingleEvent>() {
  override val initialState = ChapterDetailViewState.initial()

  private val intentS = PublishRelay.create<ChapterDetailViewIntent>()

  private val currentChapterLink
    get() = state.value.detail?.chapterLink ?: error("State is null")

  private val nextChapterLink
    get() = when (val detail = state.value.detail) {
      is ChapterDetailViewState.Detail.Data -> detail.nextChapterLink
      else -> null
    }

  private val prevChapterLink
    get() = when (val detail = state.value.detail) {
      is ChapterDetailViewState.Detail.Data -> detail.prevChapterLink
      else -> null
    }

  private val refreshProcessor =
    ObservableTransformer<ChapterDetailViewIntent.Refresh, ChapterDetailPartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .refresh(chapterLink = currentChapterLink)
          .doOnNext {
            when (it) {
              is ChapterDetailPartialChange.RefreshPartialChange.Error -> sendMessageEvent(message = "Refresh error occurred: ${it.error.getMessage()}")
              is ChapterDetailPartialChange.RefreshPartialChange.Success -> sendMessageEvent(message = "Refresh success")
            }
          }
          .cast<ChapterDetailPartialChange>()
      }
    }

  private val retryProcessor =
    ObservableTransformer<ChapterDetailViewIntent.Retry, ChapterDetailPartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .getChapterDetail(chapterLink = currentChapterLink)
          .doOnNext {
            if (it is ChapterDetailPartialChange.Initial_Retry_LoadChapter_PartialChange.Error) {
              sendMessageEvent(message = "Retry error occurred: ${it.error.getMessage()}")
            }
          }
          .cast<ChapterDetailPartialChange>()
      }
    }

  private val loadChapterProcessor =
    ObservableTransformer<ChapterDetailViewIntent.LoadChapter, ChapterDetailPartialChange> { intents ->
      intents.switchMap { intent ->
        interactor
          .getChapterDetail(chapterLink = intent.link)
          .doOnNext {
            if (it is ChapterDetailPartialChange.Initial_Retry_LoadChapter_PartialChange.Error) {
              sendMessageEvent(message = "Load ${intent.name} error occurred: ${it.error.getMessage()}")
            }
          }
      }
    }

  private val loadNextChapterProcessor =
    ObservableTransformer<ChapterDetailViewIntent.LoadNextChapter, ChapterDetailPartialChange> { intents ->
      intents
        .map { nextChapterLink.toOptional() }
        .ofType<Some<String>>()
        .exhaustMap {
          interactor
            .getChapterDetail(chapterLink = it.value)
            .doOnNext {
              if (it is ChapterDetailPartialChange.Initial_Retry_LoadChapter_PartialChange.Error) {
                sendMessageEvent(message = "Load next chapter error occurred: ${it.error.getMessage()}")
              }
            }
            .cast<ChapterDetailPartialChange>()
        }
    }

  private val loadPrevChapterProcessor =
    ObservableTransformer<ChapterDetailViewIntent.LoadPrevChapter, ChapterDetailPartialChange> { intents ->
      intents
        .map { prevChapterLink.toOptional() }
        .ofType<Some<String>>()
        .exhaustMap {
          interactor
            .getChapterDetail(chapterLink = it.value)
            .doOnNext {
              if (it is ChapterDetailPartialChange.Initial_Retry_LoadChapter_PartialChange.Error) {
                sendMessageEvent(message = "Load prev chapter error occurred: ${it.error.getMessage()}")
              }
            }
            .cast<ChapterDetailPartialChange>()
        }
    }

  private val intentToChanges =
    ObservableTransformer<ChapterDetailViewIntent, ChapterDetailPartialChange> { intents ->
      Observable.mergeArray(
        Observable.mergeArray(
          intents.ofType<ChapterDetailViewIntent.LoadChapter>(),
          intents.ofType<ChapterDetailViewIntent.Initial>()
            .singleOrError()
            .toObservable()
            .map {
              ChapterDetailViewIntent.LoadChapter(
                link = it.link,
                name = it.name
              )
            }
        ).distinctUntilChanged().compose(loadChapterProcessor),
        intents.ofType<ChapterDetailViewIntent.Refresh>().compose(refreshProcessor),
        intents.ofType<ChapterDetailViewIntent.Retry>().compose(retryProcessor),
        intents.ofType<ChapterDetailViewIntent.LoadNextChapter>().compose(loadNextChapterProcessor),
        intents.ofType<ChapterDetailViewIntent.LoadPrevChapter>().compose(loadPrevChapterProcessor)
      )
    }

  override fun processIntents(intents: Observable<ChapterDetailViewIntent>) =
    intents.subscribe(intentS)!!

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToChanges)
      .scan(initialState) { vs, change -> change.reducer(vs) }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  /**
   * Send [message]
   */
  private fun sendMessageEvent(message: String) =
    sendEvent(ChapterDetailSingleEvent.MessageEvent(message))

  private companion object {
    /**
     * Only take 1 [ChapterDetailViewIntent.Initial]
     */
    @JvmStatic
    private val intentFilter = ObservableTransformer<ChapterDetailViewIntent, ChapterDetailViewIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared
            .ofType<ChapterDetailViewIntent.Initial>()
            .take(1),
          shared.notOfType<ChapterDetailViewIntent.Initial, ChapterDetailViewIntent>()
        )
      }
    }
  }
}