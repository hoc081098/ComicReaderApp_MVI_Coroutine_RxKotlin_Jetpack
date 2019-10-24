package com.hoc.comicapp.activity.main

import com.hoc.comicapp.activity.main.MainContract.Interactor
import com.hoc.comicapp.activity.main.MainContract.PartialChange
import com.hoc.comicapp.activity.main.MainContract.SingleEvent
import com.hoc.comicapp.activity.main.MainContract.ViewIntent
import com.hoc.comicapp.activity.main.MainContract.ViewState
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Observable.mergeArray
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class MainVM(
  private val interactor: Interactor,
  rxSchedulerProvider: RxSchedulerProvider
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<ViewIntent>()

  override fun processIntents(intents: Observable<ViewIntent>) = intents.subscribe(intentS)!!

  private val initialProcessor =
    ObservableTransformer<ViewIntent.Initial, PartialChange> { intent ->
      intent.flatMap {
        interactor
          .userChanges()
          .doOnNext {
            if (it is PartialChange.User.Error) {
              sendEvent(SingleEvent.GetUserError(it.error))
            }
          }
      }
    }

  private val signOutProcessor =
    ObservableTransformer<ViewIntent.SignOut, PartialChange> { intent ->
      intent.exhaustMap {
        interactor
          .signOut()
          .doOnNext {
            when (it) {
              PartialChange.SignOut.UserSignedOut -> sendEvent(SingleEvent.SignOutSuccess)
              is PartialChange.SignOut.Error -> sendEvent(SingleEvent.SignOutFailure(it.error))
            }
          }
      }
    }

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intent ->
    intent.publish { shared ->
      mergeArray(
        shared.ofType<ViewIntent.Initial>().compose(initialProcessor),
        shared.ofType<ViewIntent.SignOut>().compose(signOutProcessor)
      )
    }
  }

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToChanges)
      .scan(initialState) { vs, change -> change.reducer(vs) }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  private companion object {
    val intentFilter = ObservableTransformer<ViewIntent, ViewIntent> { intent ->
      intent.publish { shared ->
        mergeArray(
          shared.ofType<ViewIntent.Initial>().take(1),
          shared.notOfType<ViewIntent.Initial, ViewIntent>()
        )
      }
    }
  }
}