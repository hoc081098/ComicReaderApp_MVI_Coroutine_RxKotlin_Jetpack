package com.hoc.comicapp.ui.login

import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.ui.login.LoginContract.Interactor
import com.hoc.comicapp.ui.login.LoginContract.PartialChange
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.rxObservable

@OptIn(ExperimentalCoroutinesApi::class)
class LoginInteractorImpl(
  private val userRepository: UserRepository,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
) : Interactor {
  override fun login(email: String, password: String): Observable<PartialChange> {
    return rxObservable(dispatchersProvider.main) {
      send(PartialChange.Loading)

      userRepository
        .login(email, password)
        .fold(
          ifLeft = { PartialChange.LoginFailure(it) },
          ifRight = { PartialChange.LoginSuccess }
        )
        .let { send(it) }
    }
  }
}
