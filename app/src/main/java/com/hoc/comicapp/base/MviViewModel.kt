package com.hoc.comicapp.base

import androidx.annotation.CheckResult
import androidx.lifecycle.LiveData
import com.hoc.comicapp.utils.Event
import com.shopify.livedataktx.LiveDataKtx
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

/**
 * Object that will subscribes to a MviView's [Intent]s,
 * process it and emit a [ViewState] back.
 *
 * @param I Top class of the [Intent] that the [MviViewModel] will be subscribing to.
 * @param S Top class of the [ViewState] the [MviViewModel] will be emitting.
 * @param E Top class of the [SingleEvent] that the [MviViewModel] will be emitting.
 */
interface MviViewModel<I : Intent, S : ViewState, E : SingleEvent> {
  val state: LiveDataKtx<S>

  val singleEvent: LiveData<Event<E>>

  @CheckResult fun processIntents(intents: Observable<I>): Disposable
}