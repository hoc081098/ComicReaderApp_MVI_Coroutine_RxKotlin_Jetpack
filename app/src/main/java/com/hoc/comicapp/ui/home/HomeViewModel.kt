package com.hoc.comicapp.ui.home

import android.util.Log
import com.hoc.comicapp.*
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.NetworkError
import com.hoc.comicapp.data.models.ServerError
import com.hoc.comicapp.data.models.UnexpectedError
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable
import com.hoc.comicapp.data.models.Error as RepoError

fun getMessageFromError(error: RepoError): String {
  return when (error) {
    NetworkError -> "Network error"
    is ServerError -> "Server error: ${error.message}"
    is UnexpectedError -> "An unexpected error occurred"
  }
}

data class HomeViewState(
  val suggestComics: List<Comic> = emptyList(),
  val suggestLoading: Boolean = true,
  val suggestErrorMessage: String? = null,

  val topMonthComics: List<Comic> = emptyList(),
  val topMonthLoading: Boolean = true,
  val topMonthErrorMessage: String? = null,

  val updatedComics: List<Comic> = emptyList(),
  val updatedLoading: Boolean = true,
  val updatedErrorMessage: String? = null
) : ViewState {
  companion object {
    fun initialState() = HomeViewState()
  }
}

sealed class HomeViewIntent : Intent {
  object Initial : HomeViewIntent()
  object Refresh : HomeViewIntent()
}

sealed class HomePartialChange {
  sealed class SuggestHomePartialChange : HomePartialChange() {
    data class Data(val comics: List<Comic>) : SuggestHomePartialChange()
    object Loading : SuggestHomePartialChange()
    data class Error(val error: RepoError) : SuggestHomePartialChange()
  }

  sealed class TopMonthHomePartialChange : HomePartialChange() {
    data class Data(val comics: List<Comic>) : TopMonthHomePartialChange()
    object Loading : TopMonthHomePartialChange()
    data class Error(val error: RepoError) : TopMonthHomePartialChange()
  }
}

sealed class HomeSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : HomeSingleEvent()
}

@ExperimentalCoroutinesApi
class HomeViewModel(
  dispatcherProvider: CoroutinesDispatcherProvider,
  private val comicRepository: ComicRepository
) : BaseViewModel<HomeViewIntent, HomeViewState, HomeSingleEvent>() {
  private val intentsSubject = PublishRelay.create<HomeViewIntent>()

  override val initialState = HomeViewState.initialState()

  private val initialProcessor =
    ObservableTransformer<HomeViewIntent.Initial, HomePartialChange> {
      it.flatMap {
        val suggestChanges = rxObservable {
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

        val topMonthChanges = rxObservable {
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

        Observable.mergeArray(
          suggestChanges
            .doOnNext {
              if (it is HomePartialChange.SuggestHomePartialChange.Error) {
                singleEventD.value =
                  "Get suggest list error: ${getMessageFromError(it.error)}".let {
                    HomeSingleEvent.MessageEvent(it)
                  }.let(::Event)
              }
            },
          topMonthChanges
            .doOnNext {
              if (it is HomePartialChange.TopMonthHomePartialChange.Error) {
                singleEventD.value =
                  "Get top month list error: ${getMessageFromError(it.error)}".let {
                    HomeSingleEvent.MessageEvent(it)
                  }.let(::Event)
              }
            }
        )
      }
    }
  private val refreshProcessor =
    ObservableTransformer<HomeViewIntent.Refresh, HomePartialChange> { Observable.empty() }

  private val intentToViewState = ObservableTransformer<HomeViewIntent, HomeViewState> {
    it.publish { shared ->
      Observable.mergeArray(
        shared
          .ofType<HomeViewIntent.Initial>()
          .compose(initialProcessor),
        shared
          .ofType<HomeViewIntent.Refresh>()
          .compose(refreshProcessor)
      )
    }.scan(HomeViewState.initialState(), ::reducer)
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
  }

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

  private val compositeDisposable = CompositeDisposable()

  override fun onCleared() {
    super.onCleared()
    compositeDisposable.clear()
    Log.d("@@@", "onCleared")
  }

  override fun processIntents(intents: Observable<HomeViewIntent>) =
    intents.subscribe(intentsSubject::accept)!!

  init {
    val tag = "@@@"

    intentsSubject
      .doOnNext { Log.d(tag, "intent[1]=$it") }
      .compose(intentFilter)
      .doOnNext { Log.d(tag, "intent[2]=$it") }
      .compose(intentToViewState)
      .doOnNext { Log.d(tag, "view_state=$it") }
      .subscribeBy(onNext = stateD::setValueIfNew)
      .addTo(compositeDisposable)
  }

  private fun reducer(state: HomeViewState, change: HomePartialChange): HomeViewState {
    return when (change) {
      is HomePartialChange.SuggestHomePartialChange -> when (change) {
        is HomePartialChange.SuggestHomePartialChange.Data -> state.copy(
          suggestComics = change.comics,
          suggestLoading = false,
          suggestErrorMessage = null
        )
        HomePartialChange.SuggestHomePartialChange.Loading -> state.copy(suggestLoading = true)
        is HomePartialChange.SuggestHomePartialChange.Error -> state.copy(
          suggestLoading = false,
          suggestErrorMessage = getMessageFromError(change.error)
        )
      }
      is HomePartialChange.TopMonthHomePartialChange -> when (change) {
        is HomePartialChange.TopMonthHomePartialChange.Data -> state.copy(
          topMonthComics = change.comics,
          topMonthLoading = false,
          topMonthErrorMessage = null
        )
        HomePartialChange.TopMonthHomePartialChange.Loading -> state.copy(topMonthLoading = true)
        is HomePartialChange.TopMonthHomePartialChange.Error -> state.copy(
          topMonthLoading = false,
          topMonthErrorMessage = getMessageFromError(change.error)
        )
      }
    }
  }
}

