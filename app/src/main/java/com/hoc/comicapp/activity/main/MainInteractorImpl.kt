package com.hoc.comicapp.activity.main

import com.hoc.comicapp.activity.main.MainContract.Interactor
import com.hoc.comicapp.activity.main.MainContract.PartialChange
import com.hoc.comicapp.activity.main.MainContract.ViewState.User
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.fold
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.rxObservable

@OptIn(ExperimentalCoroutinesApi::class)
class MainInteractorImpl(
  private val userRepository: UserRepository,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val rxSchedulerProvider: RxSchedulerProvider,
) : Interactor {
  override fun userChanges(): Observable<PartialChange> {
    return userRepository
      .userObservable()
      .map<PartialChange> { either ->
        either.fold(
          left = { PartialChange.User.Error(it) },
          right = {
            val user = it?.let(::User)
            PartialChange.User.UserChanged(user)
          }
        )
      }
      .observeOn(rxSchedulerProvider.main)
      .startWithItem(PartialChange.User.Loading)
  }

  override fun signOut(): Observable<PartialChange> {
    return rxObservable(dispatchersProvider.main) {
      userRepository
        .signOut()
        .fold(
          left = { PartialChange.SignOut.Error(it) },
          right = { PartialChange.SignOut.UserSignedOut }
        )
        .let { send(it) }
    }
  }
}
