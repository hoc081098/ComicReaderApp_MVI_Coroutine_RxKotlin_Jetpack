package com.hoc.comicapp.base

import androidx.annotation.CallSuper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hoc.comicapp.utils.Event
import com.hoc.comicapp.utils.NotNullLiveData
import com.hoc.comicapp.utils.NotNullMutableLiveData
import io.reactivex.rxjava3.disposables.CompositeDisposable
import timber.log.Timber

abstract class BaseViewModel<
  I : MviIntent,
  S : MviViewState,
  E : MviSingleEvent,
  >(protected val initialState: S) :
  ViewModel(),
  MviViewModel<I, S, E> {
  protected val compositeDisposable = CompositeDisposable()

  /**
   * ViewState
   */
  private val stateD = NotNullMutableLiveData(initialState)
  override val state: NotNullLiveData<S> get() = stateD

  /**
   * Single event
   * Like: snackbar message, navigation event or a dialog trigger
   */
  private val singleEventD = MutableLiveData<Event<E>>()
  override val singleEvent: LiveData<Event<E>> get() = singleEventD

  init {
    @Suppress("LeakingThis")
    Timber.d("$this::init")
  }

  protected val setNewState = { state: S ->
    if (state != stateD.value) {
      stateD.value = state
    }
  }

  protected fun sendEvent(event: E) {
    singleEventD.value = Event(event)
  }

  @CallSuper
  override fun onCleared() {
    compositeDisposable.dispose()
    Timber.d("$this::onCleared")
  }
}
