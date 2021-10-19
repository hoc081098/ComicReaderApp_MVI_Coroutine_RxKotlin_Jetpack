package com.hoc.comicapp.ui.login

import android.util.Patterns
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.login.LoginContract.Intent
import com.hoc.comicapp.ui.login.LoginContract.Interactor
import com.hoc.comicapp.ui.login.LoginContract.PartialChange
import com.hoc.comicapp.ui.login.LoginContract.SingleEvent
import com.hoc.comicapp.ui.login.LoginContract.ViewState
import com.hoc.comicapp.utils.exhaustMap
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy

class LoginVM(
  private val interactor: Interactor,
  private val rxSchedulerProvider: RxSchedulerProvider,
) : BaseViewModel<Intent, ViewState, SingleEvent>(ViewState.initial()) {

  private val intentS = PublishRelay.create<Intent>()

  override fun processIntents(intents: Observable<Intent>): Disposable = intents.subscribe(intentS)

  init {
    val emailObservable = intentS.ofType<Intent.EmailChanged>()
      .map { it.email }
      .share()
    val passwordObservable = intentS.ofType<Intent.PasswordChange>()
      .map { it.password }
      .share()

    val emailErrorChanges = emailObservable.map { PartialChange.EmailError(getEmailError(it)) }
    val passwordErrorChange =
      passwordObservable.map { PartialChange.PasswordError(getPasswordError(it)) }

    val submit = intentS
      .ofType<Intent.SubmitLogin>()
      .withLatestFrom(emailObservable, passwordObservable) { _, email, password ->
        email to password
      }

    val loginChanges = submit
      .filter { (email, password) ->
        getEmailError(email) === null && getPasswordError(password) === null
      }
      .exhaustMap { (email, password) ->
        interactor
          .login(email, password)
          .observeOn(rxSchedulerProvider.main)
          .doOnNext {
            when (it) {
              PartialChange.LoginSuccess -> sendEvent(SingleEvent.LoginSuccess)
              is PartialChange.LoginFailure -> sendEvent(SingleEvent.LoginFailure(it.error))
              is PartialChange.EmailError -> Unit
              is PartialChange.PasswordError -> Unit
              is PartialChange.EmailChanged -> Unit
              is PartialChange.PasswordChanged -> Unit
              PartialChange.Loading -> Unit
            }
          }
      }

    val emailChange = emailObservable.map { PartialChange.EmailChanged(it) }
    val passwordChange = passwordObservable.map { PartialChange.PasswordChanged(it) }

    Observable.mergeArray(
      emailErrorChanges,
      passwordErrorChange,
      loginChanges,
      emailChange,
      passwordChange
    )
      .scan(initialState) { state, change -> change.reducer(state) }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = setNewState)
      .addTo(compositeDisposable)
  }

  /**
   * @return error message or null if email is valid
   */
  private fun getEmailError(email: String): String? {
    return if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      null
    } else {
      "Invalid email address"
    }
  }

  /**
   * @return error message or null if password is valid
   */
  private fun getPasswordError(password: String): String? {
    return if (password.length < 6) {
      "Min length of password is 6"
    } else {
      null
    }
  }
}
