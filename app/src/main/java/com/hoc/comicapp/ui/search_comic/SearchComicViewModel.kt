package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.search_comic.SearchComicContract.Interactor
import com.hoc.comicapp.ui.search_comic.SearchComicContract.PartialChange
import com.hoc.comicapp.ui.search_comic.SearchComicContract.SingleEvent
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewIntent
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState
import com.hoc.comicapp.utils.exhaustMap
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.withLatestFrom
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SearchComicViewModel(
  private val interactor: Interactor,
  rxSchedulerProvider: RxSchedulerProvider,
) :
  BaseViewModel<ViewIntent, ViewState, SingleEvent>(ViewState.initialState()) {
  private val intentS = PublishRelay.create<ViewIntent>()

  override fun processIntents(intents: Observable<ViewIntent>): Disposable =
    intents.subscribe(intentS)

  init {
    val stateS = BehaviorRelay.createDefault(initialState)

    val searchTerm = intentS
      .ofType<ViewIntent.SearchIntent>()
      .map { it.term }
      .filter { it.isNotBlank() }
      .doOnNext { Timber.d("[SEARCH-1] $it") }
      .debounce(600, TimeUnit.MILLISECONDS)
      .distinctUntilChanged()
      .doOnNext { Timber.d("[SEARCH-2] $it") }
      .share()

    val retryPartialChange = intentS
      .ofType<ViewIntent.RetryFirstIntent>()
      .withLatestFrom(searchTerm)
      .map { it.second }
      .doOnNext { Timber.d("[RETRY] $it") }
      .switchMap { term ->
        interactor
          .searchComic(term, page = 1)
          .doOnNext {
            val messageFromError =
              (it as? PartialChange.FirstPage.Error ?: return@doOnNext).error.getMessage()
            sendEvent(
              SingleEvent.MessageEvent(
                "Search for '$term', error occurred: $messageFromError"
              )
            )
          }
      }
    val searchPartialChange = searchTerm
      .switchMap { term ->
        interactor
          .searchComic(term, page = 1)
          .doOnNext {
            val messageFromError =
              (it as? PartialChange.FirstPage.Error ?: return@doOnNext).error.getMessage()
            sendEvent(
              SingleEvent.MessageEvent(
                "Retry search for '$term', error occurred: $messageFromError"
              )
            )
          }
      }

    val loadNextPagePartialChange = intentS
      .ofType<ViewIntent.LoadNextPage>()
      .withLatestFrom(searchTerm, BiFunction { _, term -> term })
      .withLatestFrom(stateS)
      .map { it.first to it.second.page + 1 }
      .doOnNext { Timber.d("[LOAD NEXT PAGE] $it") }
      .exhaustMap { (term, page) ->
        interactor
          .searchComic(term, page)
          .doOnNext {
            val messageFromError =
              (it as? PartialChange.NextPage.Error ?: return@doOnNext).error.getMessage()
            sendEvent(
              SingleEvent.MessageEvent(
                "Load next page, error occurred: $messageFromError"
              )
            )
          }
      }

    val retryNextPagePartialChange = intentS
      .ofType<ViewIntent.RetryNextPage>()
      .withLatestFrom(searchTerm, BiFunction { _, term -> term })
      .withLatestFrom(stateS)
      .map { it.first to it.second.page + 1 }
      .doOnNext { Timber.d("[RETRY NEXT PAGE] $it") }
      .exhaustMap { (term, page) ->
        interactor
          .searchComic(term, page)
          .doOnNext {
            val messageFromError =
              (it as? PartialChange.NextPage.Error ?: return@doOnNext).error.getMessage()
            sendEvent(
              SingleEvent.MessageEvent(
                "Retry Load next page, error occurred: $messageFromError"
              )
            )
          }
      }

    /**
     * Subscribe
     */

    Observable.mergeArray(
      searchPartialChange,
      retryPartialChange,
      loadNextPagePartialChange,
      retryNextPagePartialChange
    )
      .scan(initialState) { state, change -> change.reducer(state) }
      .distinctUntilChanged()
      .observeOn(rxSchedulerProvider.main)
      .subscribe(stateS)
      .addTo(compositeDisposable)

    stateS.subscribeBy(onNext = setNewState).addTo(compositeDisposable)
  }
}
