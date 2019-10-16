package com.hoc.comicapp.ui.login

import android.util.Patterns
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.login.LoginContract.*
import com.hoc.comicapp.utils.exhaustMap
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom

class LoginVM(
  private val interactor: Interactor,
  private val rxSchedulerProvider: RxSchedulerProvider
) : BaseViewModel<Intent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<Intent>()

  override fun processIntents(intents: Observable<Intent>) = intents.subscribe(intentS)!!

  init {
    val emailObservable = intentS.ofType<Intent.EmailChanged>().share()
    val emailErrorChanges = emailObservable
      .map { it.email }
      .map { PartialChange.EmailError(getEmailError(it)) }

    val passwordObservable = intentS.ofType<Intent.PasswordChange>().share()
    val passwordErrorChange = passwordObservable
      .map { it.password }
      .map { PartialChange.PasswordError(getPasswordError(it)) }

    val submit = intentS
      .ofType<Intent.SubmitLogin>()
      .withLatestFrom(emailObservable, passwordObservable) { _, email, password ->
        email.email to password.password
      }

    val loginChanges = submit
      .filter { (email, password) ->
        getEmailError(email) === null && getPasswordError(password) === null
      }
      .exhaustMap { (email, password) ->
        interactor.login(
          email,
          password
        )
      }

    Observable.mergeArray(
      emailErrorChanges,
      passwordErrorChange,
      loginChanges
    ).scan(initialState) { state, change -> change.reducer(state) }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  private fun getEmailError(email: String): String? {
    return if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      null
    } else {
      "Invalid email address"
    }
  }

  private fun getPasswordError(password: String): String? {
    return if (password.length < 6) {
      "Min length of password is 6"
    } else {
      null
    }
  }
}