package com.hoc.comicapp.ui.register

import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.register.RegisterContract.Interactor
import com.hoc.comicapp.ui.register.RegisterContract.PartialChange
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class RegisterInteractorImpl(
  private val userRepository: UserRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : Interactor {
  override fun register(
    email: String,
    password: String,
    fullName: String
  ): Observable<PartialChange> {
    return rxObservable(dispatcherProvider.ui) {
      send(PartialChange.Loading)

      userRepository
        .register(email, password, fullName)
        .fold(
          left = { PartialChange.RegisterFailure(it) },
          right = { PartialChange.RegisterSuccess }
        )
        .let { send(it) }
    }
  }
}