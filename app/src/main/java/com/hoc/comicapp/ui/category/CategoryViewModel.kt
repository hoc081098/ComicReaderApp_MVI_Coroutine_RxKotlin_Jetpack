package com.hoc.comicapp.ui.category

import androidx.lifecycle.viewModelScope
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.utils.Event
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class CategoryViewModel(private val categoryInteractor: CategoryInteractor) :
  BaseViewModel<CategoryViewIntent, CategoryViewState, CategorySingleEvent>() {
  override val initialState = CategoryViewState.initialState()
  private val intentS = PublishRelay.create<CategoryViewIntent>()
  private val compositeDisposable = CompositeDisposable()

  /**
   * Filters intent by type, then compose with [ObservableTransformer] to transform [CategoryViewIntent] to [CategoryPartialChange].
   * Then using [Observable.scan] operator with reducer to transform [CategoryPartialChange]s to [CategoryViewState]
   */
  private val intentToViewState = ObservableTransformer<CategoryViewIntent, CategoryViewState> {
    it.publish { shared ->
      Observable.mergeArray(
        shared
          .ofType<CategoryViewIntent.Initial>()
          .compose(initialProcessor),
        shared
          .ofType<CategoryViewIntent.Refresh>()
          .compose(refreshProcessor),
        shared
          .ofType<CategoryViewIntent.Retry>()
          .compose(retryProcessor)
      )
    }.scan(initialState) { state, change -> change.reducer(state) }
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
  }

  /**
   * Transform [CategoryViewIntent.Initial]s to [CategoryPartialChange]s
   */
  private val initialProcessor =
    ObservableTransformer<CategoryViewIntent.Initial, CategoryPartialChange> { intentObservable ->
      intentObservable.flatMap {
        categoryInteractor.getAllCategories(viewModelScope)
          .doOnNext {
            if (it is CategoryPartialChange.Error) {
              sendMessageEvent(message = "Error occurred: ${it.error.getMessage()}")
            }
          }
      }
    }

  /**
   * Transform [CategoryViewIntent.Refresh]s to [CategoryPartialChange]s
   */
  private val refreshProcessor =
    ObservableTransformer<CategoryViewIntent.Refresh, CategoryPartialChange> { intentObservable ->
      intentObservable.exhaustMap {
        categoryInteractor.getAllCategories(viewModelScope)
          .doOnNext {
            if (it is CategoryPartialChange.Error) {
              sendMessageEvent(message = "Refresh error occurred: ${it.error.getMessage()}")
            }
          }
          .notOfType<CategoryPartialChange.Error, CategoryPartialChange>() // if refresh not successfully, not emit error change
      }
    }

  /**
   * Transform [CategoryViewIntent.Retry]s to [CategoryPartialChange]s
   */
  private val retryProcessor =
    ObservableTransformer<CategoryViewIntent.Retry, CategoryPartialChange> { intentObservable ->
      intentObservable.exhaustMap {
        categoryInteractor.getAllCategories(viewModelScope)
          .doOnNext {
            if (it is CategoryPartialChange.Error) {
              sendMessageEvent(message = "Retry error occurred: ${it.error.getMessage()}")
            }
          }
      }
    }

  override fun processIntents(intents: Observable<CategoryViewIntent>) = intents.subscribe(intentS)!!

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToViewState)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  /**
   * Send [message]
   */
  private fun sendMessageEvent(message: String) = sendEvent(Event(CategorySingleEvent.MessageEvent(message)))

  override fun onCleared() {
    super.onCleared()
    compositeDisposable.dispose()
  }

  private companion object {
    /**
     * Only take 1 [HomeViewIntent.Initial]
     */
    @JvmStatic
    private val intentFilter = ObservableTransformer<CategoryViewIntent, CategoryViewIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared
            .ofType<CategoryViewIntent.Initial>()
            .take(1),
          shared.notOfType<CategoryViewIntent.Initial, CategoryViewIntent>()
        )
      }
    }
  }
}