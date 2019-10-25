package com.hoc.comicapp.ui.favorite_comics

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.Interactor
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.PartialChange
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.SingleEvent
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ViewIntent
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ViewState
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class FavoriteComicsVM(
  private val interactor: Interactor,
  rxSchedulerProvider: RxSchedulerProvider
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<ViewIntent>()

  override fun processIntents(intents: Observable<ViewIntent>) = intents.subscribe(intentS)!!

  private val initialProcessor =
    ObservableTransformer<ViewIntent.Initial, PartialChange> { intent ->
      intent.flatMap {
        interactor
          .getFavoriteComics()
          .doOnNext {
            if (it is PartialChange.Error) {
              sendEvent(SingleEvent.Message("Error occurred: ${it.error.getMessage()}"))
            }
          }
      }
    }

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intent ->
    intent.publish {
      Observable.mergeArray(
        intent.ofType<ViewIntent.Initial>().compose(initialProcessor)
      )
    }
  }

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToChanges)
      .scan(initialState, reducer)
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  private companion object {
    val intentFilter = ObservableTransformer<ViewIntent, ViewIntent> { intent ->
      intent.publish { shared ->
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
            comics = change.comics
          )
        }
        is PartialChange.Error -> {
          vs.copy(
            isLoading = false,
            error = change.error
          )
        }
        PartialChange.Loading -> {
          vs.copy(isLoading = true)
        }
      }
    }
  }
}