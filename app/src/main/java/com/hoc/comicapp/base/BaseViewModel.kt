package com.hoc.comicapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hoc.comicapp.utils.Event
import com.shopify.livedataktx.LiveDataKtx
import com.shopify.livedataktx.MutableLiveDataKtx

abstract class BaseViewModel<I : Intent, S : ViewState, E : SingleEvent> : ViewModel(),
  MviViewModel<I, S, E> {

  protected abstract val initialState: S
  /**
   * ViewState
   */
  protected val stateD = MutableLiveDataKtx<S>().apply { value = initialState }
  override val state: LiveDataKtx<S> get() = stateD

  /**
   * Single event
   * Like: snackbar message, navigation event or a dialog trigger
   */
  protected val singleEventD = MutableLiveData<Event<E>>()
  override val singleEvent: LiveData<Event<E>> get() = singleEventD
}