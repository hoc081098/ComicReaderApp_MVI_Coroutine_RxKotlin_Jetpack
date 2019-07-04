package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.utils.Event
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.cast
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class ChapterDetailViewModel(private val interactor: ChapterDetailInteractor) :
  BaseViewModel<ChapterDetailViewIntent, ChapterDetailViewState, ChapterDetailSingleEvent>() {
  override val initialState = ChapterDetailViewState.initial()

  private val intentS = PublishRelay.create<ChapterDetailViewIntent>()

  private val initialProcessor =
    ObservableTransformer<ChapterDetailViewIntent.Initial, ChapterDetailPartialChange> { intents ->
      intents.flatMap {
        interactor
          .getChapterDetail(
            chapterName = it.initial.chapterName,
            chapterLink = it.initial.chapterLink,
            time = it.initial.time,
            view = it.initial.view
          )
          .doOnNext {
            if (it is ChapterDetailPartialChange.InitialRetryPartialChange.Error) {
              sendMessageEvent(message = "Error occurred: ${it.error.getMessage()}")
            }
          }
      }
    }

  private val refreshProcessor =
    ObservableTransformer<ChapterDetailViewIntent.Refresh, ChapterDetailPartialChange> {intents->
      intents.exhaustMap {
        interactor
          .refresh(chapterLink = it.link)
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
    ObservableTransformer<ChapterDetailViewIntent.Retry, ChapterDetailPartialChange> {intents->
      intents.exhaustMap {
        interactor
          .getChapterDetail(chapterLink = it.link)
          .doOnNext {
            if (it is ChapterDetailPartialChange.InitialRetryPartialChange.Error) {
              sendMessageEvent(message = "Retry error occurred: ${it.error.getMessage()}")
            }
          }
          .cast<ChapterDetailPartialChange>()
      }
    }


  private val intentToChanges =
    ObservableTransformer<ChapterDetailViewIntent, ChapterDetailPartialChange> { intents ->
      Observable.mergeArray(
        intents.ofType<ChapterDetailViewIntent.Initial>().compose(initialProcessor),
        intents.ofType<ChapterDetailViewIntent.Refresh>().compose(refreshProcessor),
        intents.ofType<ChapterDetailViewIntent.Retry>().compose(retryProcessor)
      )
    }

  private val disposable = intentS
    .compose(intentFilter)
    .compose(intentToChanges)
    .scan(initialState) { vs, change -> change.reducer(vs) }
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeBy(onNext = ::setNewState)

  override fun processIntents(intents: Observable<ChapterDetailViewIntent>) =
    intents.subscribe(intentS)!!


  /**
   * Send [message]
   */
  private fun sendMessageEvent(message: String) =
    sendEvent(Event(ChapterDetailSingleEvent.MessageEvent(message)))

  override fun onCleared() {
    super.onCleared()
    disposable.dispose()
  }

  private companion object {
    val intentFilter = ObservableTransformer<ChapterDetailViewIntent, ChapterDetailViewIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared.ofType<ChapterDetailViewIntent.Initial>().take(1),
          shared.notOfType<ChapterDetailViewIntent.Initial, ChapterDetailViewIntent>()
        )
      }
    }
  }
}