package com.hoc.comicapp.ui.favorite_comics

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.Interactor
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.PartialChange
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.SingleEvent
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ViewIntent
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ViewState
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy

class FavoriteComicsVM(
  private val interactor: Interactor,
  rxSchedulerProvider: RxSchedulerProvider,
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>(ViewState.initial()) {

  private val intentS = PublishRelay.create<ViewIntent>()

  override fun processIntents(intents: Observable<ViewIntent>): Disposable =
    intents.subscribe(intentS)

  private val initialProcessor =
    ObservableTransformer<ViewIntent.Initial, PartialChange> { intent ->
      intent.flatMap {
        interactor
          .getFavoriteComics()
          .doOnNext {
            if (it is PartialChange.FavoriteComics.Error) {
              sendEvent(SingleEvent.Message("Error occurred: ${it.error.getMessage()}"))
            }
          }
      }
    }

  private val removeProcessor =
    ObservableTransformer<ViewIntent.Remove, PartialChange> { intent ->
      intent
        .map { it.item }
        .groupBy { it.url }
        .flatMap { itemObservable ->
          itemObservable
            .exhaustMap {
              interactor
                .remove(it)
                .doOnNext { change ->
                  if (change is PartialChange.Remove) {
                    when (change) {
                      is PartialChange.Remove.Success -> {
                        sendEvent(SingleEvent.Message("Removed ${change.item.title}"))
                      }
                      is PartialChange.Remove.Failure -> {
                        sendEvent(SingleEvent.Message("Remove ${change.item.title} failure"))
                      }
                    }
                  }
                }
            }
        }
    }

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intent ->
    intent.publish {
      Observable.mergeArray(
        intent.ofType<ViewIntent.Initial>().compose(initialProcessor),
        intent.ofType<ViewIntent.Remove>().compose(removeProcessor)
      )
    }
  }

  init {
    intentS
      .compose(intentFilter)
      .publish { filteredIntent ->
        Observable.combineLatest(
          filteredIntent
            .ofType<ViewIntent.ChangeSortOrder>()
            .map { it.sortOrder }
            .distinctUntilChanged(),
          filteredIntent
            .compose(intentToChanges)
            .scan(initialState, reducer)
        ) { sortOrder, viewState ->
          viewState.copy(
            comics = viewState
              .comics
              .sortedWith(sortOrder.comparator),
            sortOrder = sortOrder
          )
        }
      }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = setNewState)
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
        is PartialChange.FavoriteComics.Data -> {
          vs.copy(
            isLoading = false,
            error = null,
            comics = change.comics
          )
        }
        is PartialChange.FavoriteComics.Error -> {
          vs.copy(
            isLoading = false,
            error = change.error
          )
        }
        PartialChange.FavoriteComics.Loading -> {
          vs.copy(isLoading = true)
        }
        is PartialChange.Remove.Success -> vs
        is PartialChange.Remove.Failure -> vs
      }
    }
  }
}
