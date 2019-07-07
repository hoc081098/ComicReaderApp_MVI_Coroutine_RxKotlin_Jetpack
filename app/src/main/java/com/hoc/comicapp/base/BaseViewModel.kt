package com.hoc.comicapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import com.hoc.comicapp.utils.Event
import com.shopify.livedataktx.LiveDataKtx
import com.shopify.livedataktx.MutableLiveDataKtx
import com.shopify.livedataktx.toKtx

abstract class BaseViewModel<I : Intent, S : ViewState, E : SingleEvent> : ViewModel(),
  MviViewModel<I, S, E> {

  protected abstract val initialState: S
  /**
   * ViewState
   */
  private val stateD = MutableLiveDataKtx<S>().apply { value = initialState }
  private val stateDistinctD = stateD.distinctUntilChanged().toKtx()
  override val state: LiveDataKtx<S> get() = stateDistinctD

  /**
   * Single event
   * Like: snackbar message, navigation event or a dialog trigger
   */
  private val singleEventD = MutableLiveData<Event<E>>()
  override val singleEvent: LiveData<Event<E>> get() = singleEventD

  protected fun setNewState(state: S) {
    stateD.value = state
  }

  protected fun sendEvent(event: E) {
    singleEventD.value = Event(event)
  }
}