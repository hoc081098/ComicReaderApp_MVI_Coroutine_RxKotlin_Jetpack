package com.hoc.comicapp.ui.category

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.cast
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy

class CategoryViewModel(
  private val categoryInteractor: CategoryInteractor,
  rxSchedulerProvider: RxSchedulerProvider,
) :
  BaseViewModel<CategoryViewIntent, CategoryViewState, CategorySingleEvent>(CategoryViewState.initialState()) {
  private val intentS = PublishRelay.create<CategoryViewIntent>()

  /**
   * Filters intent by type, then compose with [ObservableTransformer] to transform [CategoryViewIntent] to [CategoryPartialChange].
   * Then using [Observable.scan] operator with reducer to transform [CategoryPartialChange]s to [CategoryViewState]
   */
  private val intentToViewState =
    ObservableTransformer<CategoryViewIntent, CategoryViewState> { intents ->
      Observable.combineLatest(
        intents.publish { shared ->
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
        }.scan(initialState) { state, change -> change.reducer(state) },
        intents
          .ofType<CategoryViewIntent.ChangeSortOrder>()
          .map { it.sortOrder }
          .distinctUntilChanged(),
      ) { viewState, sortOrder ->
        viewState.copy(
          categories = viewState
            .categories
            .sortedWith(
              categoryComparators[sortOrder]
                ?: return@combineLatest viewState
            ),
          sortOrder = sortOrder
        )
      }
        .distinctUntilChanged()
        .observeOn(rxSchedulerProvider.main)
    }

  /**
   * Transform [CategoryViewIntent.Initial]s to [CategoryPartialChange]s
   */
  private val initialProcessor =
    ObservableTransformer<CategoryViewIntent.Initial, CategoryPartialChange> { intentObservable ->
      intentObservable.flatMap {
        categoryInteractor.getAllCategories()
          .doOnNext {
            if (it is CategoryPartialChange.InitialRetryPartialChange.Error) {
              sendMessageEvent(message = "Error occurred: ${it.error.getMessage()}")
            }
          }
          .cast()
      }
    }

  /**
   * Transform [CategoryViewIntent.Refresh]s to [CategoryPartialChange]s
   */
  private val refreshProcessor =
    ObservableTransformer<CategoryViewIntent.Refresh, CategoryPartialChange> { intentObservable ->
      intentObservable.exhaustMap {
        categoryInteractor.refresh()
          .doOnNext {
            when (it) {
              is CategoryPartialChange.RefreshPartialChange.Error -> sendMessageEvent(message = "Refresh error occurred: ${it.error.getMessage()}")
              is CategoryPartialChange.RefreshPartialChange.Data -> sendMessageEvent(message = "Refresh success")
              CategoryPartialChange.RefreshPartialChange.Loading -> return@doOnNext
            }
          }
          .cast<CategoryPartialChange>()
      }
    }

  /**
   * Transform [CategoryViewIntent.Retry]s to [CategoryPartialChange]s
   */
  private val retryProcessor =
    ObservableTransformer<CategoryViewIntent.Retry, CategoryPartialChange> { intentObservable ->
      intentObservable.exhaustMap {
        categoryInteractor.getAllCategories()
          .doOnNext {
            if (it is CategoryPartialChange.InitialRetryPartialChange.Error) {
              sendMessageEvent(message = "Retry error occurred: ${it.error.getMessage()}")
            }
          }
          .cast<CategoryPartialChange>()
      }
    }

  override fun processIntents(intents: Observable<CategoryViewIntent>): Disposable =
    intents.subscribe(intentS)

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToViewState)
      .subscribeBy(onNext = setNewState)
      .addTo(compositeDisposable)
  }

  /**
   * Send [message]
   */
  private fun sendMessageEvent(message: String) =
    sendEvent(CategorySingleEvent.MessageEvent(message))

  private companion object {
    val categoryComparators = mapOf<String, Comparator<Category>>(
      CATEGORY_NAME_ASC to compareBy { it.name },
      CATEGORY_NAME_DESC to compareByDescending { it.name }
    )

    /**
     * Only take 1 [CategoryViewIntent.Initial]
     */
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
