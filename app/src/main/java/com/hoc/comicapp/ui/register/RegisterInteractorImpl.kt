package com.hoc.comicapp.ui.register

import com.chrynan.uri.core.Uri
import com.chrynan.uri.core.fromString
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.ui.register.RegisterContract.Interactor
import com.hoc.comicapp.ui.register.RegisterContract.PartialChange
import com.hoc.comicapp.ui.register.RegisterContract.User
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.rxObservable
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterInteractorImpl(
  private val userRepository: UserRepository,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
) : Interactor {
  override fun register(user: User): Observable<PartialChange> {
    return rxObservable(dispatchersProvider.main) {
      Timber.d("Register $user")

      send(PartialChange.Loading)

      val (email, password, fullName, avatar) = user

      userRepository
        .register(
          email = email,
          password = password,
          fullName = fullName,
          avatar = avatar?.toString()?.let(Uri::fromString),
        )
        .fold(
          ifLeft = { PartialChange.RegisterFailure(it) },
          ifRight = { PartialChange.RegisterSuccess }
        )
        .let { send(it) }
    }
  }
}
