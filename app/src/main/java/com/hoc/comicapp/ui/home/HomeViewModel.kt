package com.hoc.comicapp.ui.home

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.data.models.getMessageFromError
import com.hoc.comicapp.utils.Event
import com.hoc.comicapp.utils.Left
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.flatMap
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.getOrNull
import com.hoc.comicapp.utils.map
import com.hoc.comicapp.utils.setValueIfNew
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable
import timber.log.Timber

@ExperimentalCoroutinesApi
class HomeViewModel(private val comicRepository: ComicRepository) :
  BaseViewModel<HomeViewIntent, HomeViewState, HomeSingleEvent>() {
  override val initialState = HomeViewState.initialState()
  private val intentsSubject = PublishRelay.create<HomeViewIntent>()
  private val compositeDisposable = CompositeDisposable()

  private val initialProcessor =
    ObservableTransformer<HomeViewIntent.Initial, HomePartialChange> {

      val suggestChangesStartsWithLoading = rxObservable {
        send(HomePartialChange.SuggestHomePartialChange.Loading)

        val suggestResult = comicRepository.getSuggest()

        suggestResult
          .getOrNull()
          .orEmpty()
          .let { HomePartialChange.SuggestHomePartialChange.Data(it) }
          .let { send(it) }

        if (suggestResult is Left) {
          suggestResult
            .value
            .let { HomePartialChange.SuggestHomePartialChange.Error(it) }
            .let { send(it) }
        }
      }

      val topMonthChangesStartsWithLoading = rxObservable {
        send(HomePartialChange.TopMonthHomePartialChange.Loading)

        val topMonthResult = comicRepository.getTopMonth()

        topMonthResult
          .getOrNull()
          .orEmpty()
          .let { HomePartialChange.TopMonthHomePartialChange.Data(it) }
          .let { send(it) }

        if (topMonthResult is Left) {
          topMonthResult
            .value
            .let { HomePartialChange.TopMonthHomePartialChange.Error(it) }
            .let { send(it) }
        }
      }

      val updatedChangesStartsWithLoading = rxObservable {
        send(HomePartialChange.UpdatedPartialChange.Loading)

        val topMonthResult = comicRepository.getUpdate()

        topMonthResult
          .getOrNull()
          .orEmpty()
          .let { HomePartialChange.UpdatedPartialChange.Data(it) }
          .let { send(it) }

        if (topMonthResult is Left) {
          topMonthResult
            .value
            .let { HomePartialChange.UpdatedPartialChange.Error(it) }
            .let { send(it) }
        }
      }

      it.flatMap {
        Observable.mergeArray(
          suggestChangesStartsWithLoading
            .doOnNext {
              if (it is HomePartialChange.SuggestHomePartialChange.Error) {
                singleEventD.value =
                  "Get suggest list error: ${getMessageFromError(it.error)}"
                    .let { HomeSingleEvent.MessageEvent(it) }
                    .let(::Event)
              }
            },
          topMonthChangesStartsWithLoading
            .doOnNext {
              if (it is HomePartialChange.TopMonthHomePartialChange.Error) {
                singleEventD.value =
                  "Get top month list error: ${getMessageFromError(it.error)}"
                    .let { HomeSingleEvent.MessageEvent(it) }
                    .let(::Event)
              }
            },
          updatedChangesStartsWithLoading
            .doOnNext {
              if (it is HomePartialChange.UpdatedPartialChange.Error) {
                singleEventD.value =
                  "Get updated list error: ${getMessageFromError(it.error)}"
                    .let { HomeSingleEvent.MessageEvent(it) }
                    .let(::Event)
              }
            }
        )
      }
    }

  private val refreshProcessor =
    ObservableTransformer<HomeViewIntent.Refresh, HomePartialChange> { intent ->
      intent
        .exhaustMap {
          Observables.zip(
            rxObservable { send(comicRepository.getSuggest()) },
            rxObservable { send(comicRepository.getTopMonth()) },
            rxObservable { send(comicRepository.getUpdate()) }
          ).flatMap { (suggest, topMonth, updated) ->
            suggest.flatMap { suggestList ->
              topMonth.flatMap { topMonthList ->
                updated.map { updatedList ->
                  listOf(
                    HomePartialChange.TopMonthHomePartialChange.Data(topMonthList),
                    HomePartialChange.SuggestHomePartialChange.Data(suggestList),
                    HomePartialChange.UpdatedPartialChange.Data(updatedList),
                    HomePartialChange.RefreshSuccess
                  ).toObservable()
                }
              }
            }.fold({ Observable.just(HomePartialChange.RefreshFailure(it)) }, { it })
          }.doOnNext {
            if (it is HomePartialChange.RefreshSuccess) {
              singleEventD.value = HomeSingleEvent.MessageEvent("Refresh successfully").let(::Event)
            } else if (it is HomePartialChange.RefreshFailure) {
              singleEventD.value =
                HomeSingleEvent.MessageEvent("Refresh not successfully").let(::Event)
            }
          }
        }
    }

  private val loadNextPageProcessor =
    ObservableTransformer<HomeViewIntent.LoadNextPageUpdatedComic, HomePartialChange> { intent ->
      intent
        .filter {
          !stateD.value.items.any {
            it is HomeListItem.UpdatedItem.Error || it is HomeListItem.UpdatedItem.Loading
          }
        }
        .map { stateD.value.updatedPage }
        .doOnNext { Timber.d("load_next_page = $it") }
        .exhaustMap { currentPage ->
          rxObservable<HomePartialChange> {
            send(HomePartialChange.UpdatedPartialChange.Loading)

            val result = comicRepository.getUpdate(page = currentPage + 1)

            result
              .getOrNull()
              .orEmpty()
              .let { HomePartialChange.UpdatedPartialChange.Data(it) }
              .let { send(it) }

            if (result is Left) {
              result
                .value
                .let { HomePartialChange.UpdatedPartialChange.Error(it) }
                .let { send(it) }
            }
          }.doOnNext {
            if (it is HomePartialChange.UpdatedPartialChange.Error) {
              singleEventD.value =
                "Get updated list error: ${getMessageFromError(it.error)}"
                  .let { HomeSingleEvent.MessageEvent(it) }
                  .let(::Event)
            }
          }
        }
    }

  private val intentToViewState = ObservableTransformer<HomeViewIntent, HomeViewState> {
    it.publish { shared ->
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
          .compose(TODO()),
        TODO()
      )
    }.doOnNext { Timber.d("partial_change=$it") }
      .scan(HomeViewState.initialState()) { state, change -> change.reducer(state) }
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
  }


  override fun processIntents(intents: Observable<HomeViewIntent>) =
    intents.subscribe(intentsSubject::accept)!!

  init {
    intentsSubject
      .compose(intentFilter)
      .doOnNext { Timber.d("intent=$it") }
      .compose(intentToViewState)
      .doOnNext { Timber.d("view_state=$it") }
      .subscribeBy(onNext = stateD::setValueIfNew)
      .addTo(compositeDisposable)
  }

  override fun onCleared() {
    super.onCleared()
    compositeDisposable.clear()
    Timber.d("onCleared")
  }

  private companion object {
    @JvmStatic
    private val intentFilter = ObservableTransformer<HomeViewIntent, HomeViewIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared
            .ofType<HomeViewIntent.Initial>()
            .take(1),
          shared.filter { it !is HomeViewIntent.Initial }
        )
      }
    }
  }
}