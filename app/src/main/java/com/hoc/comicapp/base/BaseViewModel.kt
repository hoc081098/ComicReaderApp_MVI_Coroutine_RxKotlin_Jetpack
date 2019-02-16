package com.hoc.comicapp.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.coroutines.CoroutineContext

abstract class BaseViewModel<I : Intent, S : ViewState, E : SingleEvent>(
  coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel(), CoroutineScope, MviViewModel<I, S, E> {

  override fun processIntents(intents: ReceiveChannel<I>) {
    processIntents(intents, _state, _singleEvent)
  }

  /**
   * ViewState
   */
  private val _state = MutableLiveData<S>()
  override val state: LiveData<S> get() = _state

  /**
   * Single event
   * Like: snackbar message, navigation event or a dialog trigger
   */
  private val _singleEvent = MutableLiveData<Event<E>>()
  override val singleEvent: LiveData<Event<E>> get() = _singleEvent

  private val job = Job()
  /**
   * Context of this scope.
   */
  override val coroutineContext: CoroutineContext = job + coroutinesDispatcherProvider.ui

  init {

  }

  abstract fun processIntents(
    intents: ReceiveChannel<I>,
    state: MutableLiveData<S>,
    singleEvent: MutableLiveData<Event<E>>
  )

  override fun onCleared() {
    super.onCleared()
    job.cancel()
  }
}