package com.hoc.comicapp.ui.home

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.withLatestFrom
import timber.log.Timber

class HomeViewModel(
  private val homeInteractor: HomeInteractor,
  rxSchedulerProvider: RxSchedulerProvider,
) :
  BaseViewModel<HomeViewIntent, HomeViewState, HomeSingleEvent>(HomeViewState.initialState()) {

  private val intentS = PublishRelay.create<HomeViewIntent>()
  private val stateS = BehaviorRelay.createDefault(initialState)

  /**
   * Transform [HomeViewIntent.Initial]s to [HomePartialChange]s
   */
  private val initialProcessor =
    ObservableTransformer<HomeViewIntent.Initial, HomePartialChange> { intent ->
      intent
        .flatMap {
          Observable.mergeArray(
            homeInteractor
              .newestComics()
              .doOnNext {
                val messageFromError = (
                  it as? HomePartialChange.NewestHomePartialChange.Error
                    ?: return@doOnNext
                  ).error.getMessage()
                sendMessageEvent("Get newest list error: $messageFromError")
              },
            homeInteractor
              .mostViewedComics()
              .doOnNext {
                val messageFromError = (
                  it as? HomePartialChange.MostViewedHomePartialChange.Error
                    ?: return@doOnNext
                  ).error.getMessage()
                sendMessageEvent("Get most viewed list error: $messageFromError")
              },
            homeInteractor
              .updatedComics(page = 1)
              .doOnNext {
                val messageFromError =
                  (
                    it as? HomePartialChange.UpdatedPartialChange.Error
                      ?: return@doOnNext
                    ).error.getMessage()
                sendMessageEvent("Get updated list error: $messageFromError")
              }
          )
        }
    }

  /**
   * Transform [HomeViewIntent.Refresh]s to [HomePartialChange]s
   */
  private val refreshProcessor =
    ObservableTransformer<HomeViewIntent.Refresh, HomePartialChange> { intent ->
      intent
        .exhaustMap {
          homeInteractor
            .refreshAll()
            .doOnNext {
              sendMessageEvent(
                when (it) {
                  is HomePartialChange.RefreshPartialChange.RefreshSuccess -> "Refresh successfully"
                  is HomePartialChange.RefreshPartialChange.RefreshFailure -> "Refresh not successfully: ${it.error.getMessage()}"
                  else -> return@doOnNext
                }
              )
            }
        }
    }

  /**
   * Transform [HomeViewIntent.LoadNextPageUpdatedComic]s to [HomePartialChange]s
   */
  private val loadNextPageProcessor =
    ObservableTransformer<HomeViewIntent.LoadNextPageUpdatedComic, HomePartialChange> { intent ->
      intent
        .withLatestFrom(stateS)
        .filter {
          !it.second
            .items
            .any(HomeListItem::isLoadingOrError)
        }
        .map { it.second.updatedPage + 1 }
        .doOnNext { Timber.d("[~~~] load_next_page = $it") }
        .exhaustMap {
          homeInteractor.updatedComics(
            page = it
          )
        }
    }

  /**
   * Transform [HomeViewIntent.RetryUpdate]s to [HomePartialChange]s
   */
  private val retryUpdateProcessor =
    ObservableTransformer<HomeViewIntent.RetryUpdate, HomePartialChange> { intent ->
      intent
        .withLatestFrom(stateS)
        .map { it.second.updatedPage + 1 }
        .doOnNext { Timber.d("[~~~] refresh_page=$it") }
        .exhaustMap { page ->
          homeInteractor
            .updatedComics(page = page)
            .doOnNext {
              val messageFromError = (
                it as? HomePartialChange.UpdatedPartialChange.Error
                  ?: return@doOnNext
                ).error.getMessage()
              sendMessageEvent("Error when retry get updated list: $messageFromError")
            }
        }
    }

  /**
   * Transform [HomeViewIntent.RetryNewest]s to [HomePartialChange]s
   */
  private val retryNewestProcessor =
    ObservableTransformer<HomeViewIntent.RetryNewest, HomePartialChange> { intentObservable ->
      intentObservable.exhaustMap {
        homeInteractor
          .newestComics()
          .doOnNext {
            val messageFromError = (
              it as? HomePartialChange.NewestHomePartialChange.Error
                ?: return@doOnNext
              ).error.getMessage()
            sendMessageEvent("Error when retry get newest list: $messageFromError")
          }
      }
    }

  /**
   * Transform [HomeViewIntent.RetryMostViewed]s to [HomePartialChange]s
   */
  private val retryMostViewedProcessor =
    ObservableTransformer<HomeViewIntent.RetryMostViewed, HomePartialChange> { intentObservable ->
      intentObservable.exhaustMap {
        homeInteractor
          .mostViewedComics()
          .doOnNext {
            val messageFromError = (
              it as? HomePartialChange.MostViewedHomePartialChange.Error
                ?: return@doOnNext
              ).error.getMessage()
            sendMessageEvent("Error when retry get most viewed list: $messageFromError")
          }
      }
    }

  /**
   * Filters intent by type, then compose with [ObservableTransformer] to transform [HomeViewIntent] to [HomePartialChange].
   * Then using [Observable.scan] operator with reducer to transform [HomePartialChange] [HomeViewState]
   */
  private val intentToViewState =
    ObservableTransformer<HomeViewIntent, HomeViewState> { intentObservable ->
      intentObservable.publish { shared ->
        Observable.mergeArray(
          shared
            .ofType<HomeViewIntent.Initial>()
            .compose(initialProcessor),
          shared
            .ofType<HomeViewIntent.Refresh>()
            .compose(refreshProcessor),
          shared
            .ofType<HomeViewIntent.LoadNextPageUpdatedComic>()
            .compose(loadNextPageProcessor),
          shared
            .ofType<HomeViewIntent.RetryUpdate>()
            .compose(retryUpdateProcessor),
          shared
            .ofType<HomeViewIntent.RetryNewest>()
            .compose(retryNewestProcessor),
          shared
            .ofType<HomeViewIntent.RetryMostViewed>()
            .compose(retryMostViewedProcessor)
        )
      }
        .doOnNext { Timber.d("partial_change=$it") }
        .scan(initialState) { state, change -> change.reducer(state) }
        .distinctUntilChanged()
        .observeOn(rxSchedulerProvider.main)
    }

  override fun processIntents(intents: Observable<HomeViewIntent>): Disposable =
    intents.subscribe(intentS)

  init {
    intentS
      .compose(intentFilter)
      .doOnNext { Timber.d("intent=$it") }
      .compose(intentToViewState)
      .doOnNext { Timber.d("view_state=$it") }
      .subscribeBy(onNext = stateS::accept)
      .addTo(compositeDisposable)

    stateS
      .subscribeBy(onNext = setNewState)
      .addTo(compositeDisposable)
  }

  private fun sendMessageEvent(message: String) {
    sendEvent(HomeSingleEvent.MessageEvent(message))
  }

  private companion object {
    /**
     * Only take 1 [HomeViewIntent.Initial]
     */
    private val intentFilter = ObservableTransformer<HomeViewIntent, HomeViewIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared
            .ofType<HomeViewIntent.Initial>()
            .take(1),
          shared.notOfType<HomeViewIntent.Initial, HomeViewIntent>()
        )
      }
    }
  }
}
