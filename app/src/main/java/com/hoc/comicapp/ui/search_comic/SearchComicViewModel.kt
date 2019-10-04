package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.search_comic.SearchComicContract.*
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SearchComicViewModel(
  private val interactor: Interactor,
  rxSchedulerProvider: RxSchedulerProvider
) :
  BaseViewModel<ViewIntent, ViewState, SingleEvent>() {
  private val intentS = PublishRelay.create<ViewIntent>()

  override val initialState = ViewState.initialState()

  override fun processIntents(intents: Observable<ViewIntent>) =
    intents.subscribe(intentS)!!

  init {
    val searchTerm = intentS
      .ofType<ViewIntent.SearchIntent>()
      .map { it.term }
      .doOnNext { Timber.d("[SEARCH-1] $it") }
      .debounce(600, TimeUnit.MILLISECONDS)
      .filter { it.isNotBlank() }
      .distinctUntilChanged()
      .doOnNext { Timber.d("[SEARCH-2] $it") }

    val retryPartialChange = intentS
      .ofType<ViewIntent.RetryIntent>()
      .withLatestFrom(searchTerm)
      .map { it.second }
      .doOnNext { Timber.d("[RETRY] $it") }
      .switchMap { term ->
        interactor
          .searchComic(term)
          .doOnNext {
            val messageFromError =
              (it as? PartialChange.Error ?: return@doOnNext).error.getMessage()
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
          .searchComic(term)
          .doOnNext {
            val messageFromError =
            (it as? PartialChange.Error ?: return@doOnNext).error.getMessage()
            sendEvent(
              SingleEvent.MessageEvent(
                "Retry search for '$term', error occurred: $messageFromError"
              )
            )
          }
      }
    Observable.mergeArray(searchPartialChange, retryPartialChange)
      .scan(initialState) { state, change -> change.reducer(state) }
      .distinctUntilChanged()
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }
}