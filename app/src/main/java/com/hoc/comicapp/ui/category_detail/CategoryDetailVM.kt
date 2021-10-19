package com.hoc.comicapp.ui.category_detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.Interactor
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.SingleEvent
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewIntent
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observable.mergeArray
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy

class CategoryDetailVM(
  rxSchedulerProvider: RxSchedulerProvider,
  private val interactor: Interactor,
  category: Arguments.CategoryDetailArgs,
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>(ViewState.initial(category)) {
  private val intentS = PublishRelay.create<ViewIntent>()
  private val stateS = BehaviorRelay.createDefault(initialState)

  override fun processIntents(intents: Observable<ViewIntent>): Disposable = intents.subscribe(intentS)

  private val initialProcessor =
    ObservableTransformer<ViewIntent.Initial, PartialChange> { intent ->
      intent.flatMap { (arg) ->
        mergeArray(
          interactor.getPopulars(categoryLink = arg.link),
          interactor.getComics(categoryLink = arg.link, page = 1)
        )
      }
    }

  private val loadNextPageProcessor =
    ObservableTransformer<ViewIntent.LoadNextPage, PartialChange> { intent ->
      intent
        .withLatestFrom(stateS) { _, state -> state }
        .filter { !it.items.any(ViewState.Item::isLoadingOrError) }
        .map { it.category.link to it.page + 1 }
        .exhaustMap { (link, page) ->
          interactor.getComics(
            categoryLink = link,
            page = page
          )
        }
    }

  private val refreshProcessor =
    ObservableTransformer<ViewIntent.Refresh, PartialChange> { intent ->
      intent
        .withLatestFrom(stateS) { _, state -> state.category.link }
        .exhaustMap {
          interactor.refreshAll(categoryLink = it)
        }
    }

  private val retryPopularProcessor =
    ObservableTransformer<ViewIntent.RetryPopular, PartialChange> { intent ->
      intent
        .withLatestFrom(stateS) { _, state -> state.category.link }
        .exhaustMap { interactor.getPopulars(categoryLink = it) }
    }

  private val retryProcessor = ObservableTransformer<ViewIntent.Retry, PartialChange> { intent ->
    intent
      .withLatestFrom(stateS) { _, state -> state }
      .map { it.category.link to it.page + 1 }
      .exhaustMap { (link, page) ->
        interactor.getComics(
          categoryLink = link,
          page = page
        )
      }
  }

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intent ->
    intent.publish { shared ->
      mergeArray(
        shared.ofType<ViewIntent.Initial>().compose(initialProcessor),
        shared.ofType<ViewIntent.LoadNextPage>().compose(loadNextPageProcessor),
        shared.ofType<ViewIntent.Refresh>().compose(refreshProcessor),
        shared.ofType<ViewIntent.RetryPopular>().compose(retryPopularProcessor),
        shared.ofType<ViewIntent.Retry>().compose(retryProcessor)
      )
    }
  }

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToChanges)
      .scan(initialState) { vs, change -> change.reducer(vs) }
      .observeOn(rxSchedulerProvider.main)
      .subscribe(stateS)
      .addTo(compositeDisposable)

    stateS
      .subscribeBy(onNext = setNewState)
      .addTo(compositeDisposable)
  }

  private companion object {
    val intentFilter = ObservableTransformer<ViewIntent, ViewIntent> { intent ->
      intent.publish { shared ->
        mergeArray(
          shared.ofType<ViewIntent.Initial>().take(1),
          shared.notOfType<ViewIntent.Initial, ViewIntent>()
        )
      }
    }
  }
}
