package com.hoc.comicapp.base

import io.reactivex.Observable

/**
 * Object that will render view state,
 * handle single event from [MviViewModel]
 * and provide view intents to view model.
 *
 * @param I Top class of the [Intent] that the [MviView] will be provide
 * @param S Top class of the [ViewState] that the [MviView] will render.
 * @param E Top class of the [SingleEvent] that the [MviView] will handle.
 */
interface MviView<
    I : Intent,
    S : ViewState,
    E : SingleEvent,
    > {
  fun render(viewState: S)

  fun handleEvent(event: E)

  fun viewIntents(): Observable<I>
}