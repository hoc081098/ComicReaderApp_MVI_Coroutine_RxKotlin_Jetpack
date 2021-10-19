package com.hoc.comicapp.ui.register

import android.net.Uri
import android.util.Patterns
import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.register.RegisterContract.Intent
import com.hoc.comicapp.ui.register.RegisterContract.Interactor
import com.hoc.comicapp.ui.register.RegisterContract.PartialChange
import com.hoc.comicapp.ui.register.RegisterContract.SingleEvent
import com.hoc.comicapp.ui.register.RegisterContract.User
import com.hoc.comicapp.ui.register.RegisterContract.ViewState
import com.hoc.comicapp.utils.exhaustMap
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.kotlin.subscribeBy

class RegisterVM(
  private val interactor: Interactor,
  private val rxSchedulerProvider: RxSchedulerProvider,
) : BaseViewModel<Intent, ViewState, SingleEvent>(ViewState.initial()) {

  private val intentS = PublishRelay.create<Intent>()

  override fun processIntents(intents: Observable<Intent>): Disposable = intents.subscribe(intentS)

  init {
    val avatarSubject = BehaviorRelay.createDefault<Option<Uri>>(None)

    val emailObservable = intentS.ofType<Intent.EmailChanged>()
      .map { it.email }
      .share()
    val passwordObservable = intentS.ofType<Intent.PasswordChanged>()
      .map { it.password }
      .share()
    val fullNameObservable = intentS.ofType<Intent.FullNameChanged>()
      .map { it.fullName }
      .share()
    val avatarObservable = intentS.ofType<Intent.AvatarChanged>()
      .map { it.uri }
      .share()

    avatarObservable
      .map { it.toOption() }
      .subscribe(avatarSubject)
      .addTo(compositeDisposable)

    val emailErrorChange = emailObservable.map { PartialChange.EmailError(getEmailError(it)) }
    val passwordErrorChange =
      passwordObservable.map { PartialChange.PasswordError(getPasswordError(it)) }
    val fullNameErrorChange =
      fullNameObservable.map { PartialChange.FullNameError(getFullNameError(it)) }

    val registerChange = intentS
      .ofType<Intent.SubmitRegister>()
      .withLatestFrom(
        emailObservable,
        passwordObservable,
        fullNameObservable,
        avatarSubject
      ) { _, email, password, fullName, avatarOptional ->
        User(
          email = email,
          password = password,
          fullName = fullName,
          avatar = avatarOptional.orNull()
        )
      }
      .filter(::isValidUser)
      .exhaustMap { user ->
        interactor
          .register(user)
          .observeOn(rxSchedulerProvider.main)
          .doOnNext {
            when (it) {
              PartialChange.RegisterSuccess -> sendEvent(SingleEvent.RegisterSuccess)
              is PartialChange.RegisterFailure -> sendEvent(SingleEvent.RegisterFailure(it.error))
              is PartialChange.AvatarChanged -> return@doOnNext
              is PartialChange.EmailChanged -> return@doOnNext
              is PartialChange.EmailError -> return@doOnNext
              is PartialChange.FullNameChanged -> return@doOnNext
              is PartialChange.FullNameError -> return@doOnNext
              PartialChange.Loading -> return@doOnNext
              is PartialChange.PasswordChanged -> return@doOnNext
              is PartialChange.PasswordError -> return@doOnNext
            }
          }
      }

    val emailChange = emailObservable.map { PartialChange.EmailChanged(it) }
    val passwordChange = passwordObservable.map { PartialChange.PasswordChanged(it) }
    val fullNameChange = fullNameObservable.map { PartialChange.FullNameChanged(it) }
    val avatarChange = avatarObservable.map { PartialChange.AvatarChanged(it) }

    Observable.mergeArray(
      emailErrorChange,
      passwordErrorChange,
      fullNameErrorChange,
      avatarChange,
      registerChange,
      emailChange,
      passwordChange,
      fullNameChange
    ).scan(initialState) { state, change -> change.reducer(state) }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = setNewState)
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

  private fun isValidUser(user: User): Boolean {
    val (email, password, fullName) = user
    return getEmailError(email) === null &&
      getPasswordError(password) === null &&
      getFullNameError(fullName) === null
  }
}
