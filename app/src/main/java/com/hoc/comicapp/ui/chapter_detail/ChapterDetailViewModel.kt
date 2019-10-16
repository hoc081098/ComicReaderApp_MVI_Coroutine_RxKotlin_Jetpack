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
import io.reactivex.rxkotlin.*

class ChapterDetailViewModel(
  private val interactor: ChapterDetailInteractor,
  rxSchedulerProvider: RxSchedulerProvider,
  private val isDownloaded: Boolean
) :
  BaseViewModel<ChapterDetailViewIntent, ChapterDetailViewState, ChapterDetailSingleEvent>() {
  override val initialState = ChapterDetailViewState.initial()

  private val intentS = PublishRelay.create<ChapterDetailViewIntent>()

  private val currentChapter
    get() = state.value.detail?.chapter ?: error("State is null")

  private val nextChapter: Chapter?
    get() {
      val detail = (state.value.detail as? ChapterDetailViewState.Detail.Data) ?: return null
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
      val detail = (state.value.detail as? ChapterDetailViewState.Detail.Data) ?: return null
      return detail.prevChapterLink?.let { prevChapterLink ->
        detail
          .chapters
          .find { it.link == prevChapterLink }
          ?.name
          ?.let { Chapter(name = it, link = prevChapterLink) }
      }
    }

  private val refreshProcessor =
    ObservableTransformer<Refresh, ChapterDetailPartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .refresh(currentChapter, isDownloaded)
          .doOnNext {
            when (it) {
              is Error -> sendMessageEvent(message = "Refresh error occurred: ${it.error.getMessage()}")
              is Success -> sendMessageEvent(message = "Refresh success")
            }
          }
      }
    }

  private val retryProcessor =
    ObservableTransformer<Retry, ChapterDetailPartialChange> { intents ->
      intents.exhaustMap {
        interactor
          .getChapterDetail(currentChapter, isDownloaded)
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
            .getChapterDetail(intent.chapter, isDownloaded)
            .doOnNext {
              if (it is InitialRetryLoadChapterPartialChange.Error) {
                sendMessageEvent("Load ${intent.chapter.name} error occurred: ${it.error.getMessage()}")
              }
            }
        }
    }

  private val intentToChanges =
    ObservableTransformer<ChapterDetailViewIntent, ChapterDetailPartialChange> { intents ->
      Observable.mergeArray(
        Observable.mergeArray(
          intents.ofType(),
          intents.ofType<Initial>().map { LoadChapter(it.chapter) },
          intents.ofType<LoadNextChapter>()
            .map { nextChapter.toOptional() }
            .ofType<Some<Chapter>>()
            .map { it.value }
            .map(::LoadChapter),
          intents.ofType<LoadPrevChapter>()
            .map { prevChapter.toOptional() }
            .ofType<Some<Chapter>>()
            .map { it.value }
            .map(::LoadChapter)
        )
          .distinctUntilChanged()
          .compose(loadChapterProcessor),
        intents.ofType<Refresh>().compose(refreshProcessor),
        intents.ofType<Retry>().compose(retryProcessor)
      )
    }

  override fun processIntents(intents: Observable<ChapterDetailViewIntent>) =
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
            .ofType<ChangeOrientation>()
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