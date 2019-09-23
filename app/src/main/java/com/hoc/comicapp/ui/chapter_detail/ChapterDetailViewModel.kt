package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.InitialRetryLoadChapterPartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.RefreshPartialChange.Error
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.RefreshPartialChange.Success
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewIntent.*
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Chapter
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

  private val currentChapter
    get() = state.value.detail?.chapter ?: error("State is null")

  private val nextChapter
    get() = when (val detail = state.value.detail) {
      is ChapterDetailViewState.Detail.Data -> detail.nextChapterLink?.let { Chapter(name = "Testing...", link = it) }
      else -> null
    }

  private val prevChapter
    get() = when (val detail = state.value.detail) {
      is ChapterDetailViewState.Detail.Data -> detail.prevChapterLink?.let { Chapter(name = "Testing...", link = it) }
      else -> null
    }

  private val refreshProcessor =
    ObservableTransformer<Refresh, ChapterDetailPartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .refresh(currentChapter)
          .doOnNext {
            when (it) {
              is Error -> sendMessageEvent(message = "Refresh error occurred: ${it.error.getMessage()}")
              is Success -> sendMessageEvent(message = "Refresh success")
            }
          }
          .cast<ChapterDetailPartialChange>()
      }
    }

  private val retryProcessor =
    ObservableTransformer<Retry, ChapterDetailPartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .getChapterDetail(currentChapter)
          .doOnNext {
            if (it is InitialRetryLoadChapterPartialChange.Error) {
              sendMessageEvent(message = "Retry error occurred: ${it.error.getMessage()}")
            }
          }
          .cast<ChapterDetailPartialChange>()
      }
    }

  private val loadChapterProcessor =
    ObservableTransformer<LoadChapter, ChapterDetailPartialChange> { intents ->
      intents
        .switchMap { intent ->
          interactor
            .getChapterDetail(intent.chapter)
            .doOnNext {
              if (it is InitialRetryLoadChapterPartialChange.Error) {
                sendMessageEvent(message = "Load ${intent.chapter} error occurred: ${it.error.getMessage()}")
              }
            }
        }
    }

  private val loadNextChapterProcessor =
    ObservableTransformer<LoadNextChapter, ChapterDetailPartialChange> { intents ->
      intents
        .map { nextChapter.toOptional() }
        .ofType<Some<Chapter>>()
        .exhaustMap { nextChapterOptional ->
          interactor
            .getChapterDetail(nextChapterOptional.value)
            .doOnNext {
              if (it is InitialRetryLoadChapterPartialChange.Error) {
                sendMessageEvent(message = "Load next chapter error occurred: ${it.error.getMessage()}")
              }
            }
            .cast<ChapterDetailPartialChange>()
        }
    }

  private val loadPrevChapterProcessor =
    ObservableTransformer<LoadPrevChapter, ChapterDetailPartialChange> { intents ->
      intents
        .map { prevChapter.toOptional() }
        .ofType<Some<Chapter>>()
        .exhaustMap { prevChapterOptional ->
          interactor
            .getChapterDetail(prevChapterOptional.value)
            .doOnNext {
              if (it is InitialRetryLoadChapterPartialChange.Error) {
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
          intents.ofType(),
          intents.ofType<Initial>().map { LoadChapter(it.chapter) }
        )
          .distinctUntilChanged()
          .compose(loadChapterProcessor),
        intents.ofType<Refresh>().compose(refreshProcessor),
        intents.ofType<Retry>().compose(retryProcessor),
        intents.ofType<LoadNextChapter>().compose(loadNextChapterProcessor),
        intents.ofType<LoadPrevChapter>().compose(loadPrevChapterProcessor)
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
            .ofType<Initial>()
            .take(1),
          shared.notOfType<Initial, ChapterDetailViewIntent>()
        )
      }
    }
  }
}