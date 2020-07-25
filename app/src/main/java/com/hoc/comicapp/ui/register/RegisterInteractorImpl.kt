package com.hoc.comicapp.ui.register

import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.ui.register.RegisterContract.Interactor
import com.hoc.comicapp.ui.register.RegisterContract.PartialChange
import com.hoc.comicapp.ui.register.RegisterContract.User
import com.hoc.comicapp.utils.fold
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.rxObservable
import timber.log.Timber

@ExperimentalCoroutinesApi
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
        .register(email, password, fullName, avatar)
        .fold(
          left = { PartialChange.RegisterFailure(it) },
          right = { PartialChange.RegisterSuccess }
        )
        .let { send(it) }
    }
  }
}