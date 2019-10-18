package com.hoc.comicapp.ui.register

import android.util.Patterns
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.register.RegisterContract.Intent
import com.hoc.comicapp.ui.register.RegisterContract.Interactor
import com.hoc.comicapp.ui.register.RegisterContract.PartialChange
import com.hoc.comicapp.ui.register.RegisterContract.SingleEvent
import com.hoc.comicapp.ui.register.RegisterContract.ViewState
import com.hoc.comicapp.utils.exhaustMap
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom

class RegisterVM(
  private val interactor: Interactor,
  private val rxSchedulerProvider: RxSchedulerProvider
) : BaseViewModel<Intent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()

  private val intentS = PublishRelay.create<Intent>()

  override fun processIntents(intents: Observable<Intent>) = intents.subscribe(intentS)!!

  init {
    val emailObservable = intentS.ofType<Intent.EmailChanged>()
      .map { it.email }
      .share()
    val passwordObservable = intentS.ofType<Intent.PasswordChanged>()
      .map { it.password }
      .share()
    val fullNameObservable = intentS.ofType<Intent.FullNameChanged>()
      .map { it.fullName }
      .share()

    val emailErrorChange = emailObservable.map { PartialChange.EmailError(getEmailError(it)) }
    val passwordErrorChange =
      passwordObservable.map { PartialChange.PasswordError(getPasswordError(it)) }
    val fullNameErrorChange =
      fullNameObservable.map { PartialChange.FullNameError(getFullNameError(it)) }

    val submit = intentS
      .ofType<Intent.SubmitRegister>()
      .withLatestFrom(
        emailObservable,
        passwordObservable,
        fullNameObservable
      ) { _, email, password, fullName ->
        Triple(email, password, fullName)
      }

    val registerChange = submit
      .filter { (email, password, fullName) ->
        getEmailError(email) === null &&
            getPasswordError(password) === null &&
            getFullNameError(fullName) === null
      }
      .exhaustMap { (email, password, fullName) ->
        interactor
          .register(email, password, fullName)
          .observeOn(rxSchedulerProvider.main)
          .doOnNext {
            when (it) {
              PartialChange.RegisterSuccess -> sendEvent(SingleEvent.RegisterSuccess)
              is PartialChange.RegisterFailure -> sendEvent(SingleEvent.RegisterFailure(it.error))
            }
          }
      }

    Observable.mergeArray(
      emailErrorChange,
      passwordErrorChange,
      fullNameErrorChange,
      registerChange
    ).scan(initialState) { state, change -> change.reducer(state) }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  /**
   * @return error message or null if full name is valid
   */
  private fun getFullNameError(fullName: String): String? {
    return if (fullName.length < 3) {
      "Min length of full name is 3"
    } else {
      null
    }
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