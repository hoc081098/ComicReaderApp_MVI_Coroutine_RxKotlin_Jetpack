package com.hoc.comicapp.ui.login

import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.login.LoginContract.Interactor
import com.hoc.comicapp.ui.login.LoginContract.PartialChange
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class LoginInteractorImpl(
  private val userRepository: UserRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : Interactor {
  override fun login(email: String, password: String): Observable<PartialChange> {
    return rxObservable<PartialChange>(dispatcherProvider.ui) {
      send(PartialChange.Loading)

      userRepository
        .login(email, password)
        .fold(
          left = { PartialChange.LoginFailure(it) },
          right = { PartialChange.LoginSuccess }
        )
        .let { send(it) }
    }
  }
}