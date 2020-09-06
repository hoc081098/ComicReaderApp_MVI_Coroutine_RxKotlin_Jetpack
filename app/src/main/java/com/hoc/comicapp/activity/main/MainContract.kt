package com.hoc.comicapp.activity.main

import com.hoc.comicapp.base.MviIntent
import com.hoc.comicapp.domain.models.ComicAppError
import io.reactivex.rxjava3.core.Observable

interface MainContract {
  sealed class ViewIntent : MviIntent {
    object Initial : ViewIntent()
    object SignOut : ViewIntent()
  }

  data class ViewState(
    val user: User?,
    val isLoading: Boolean,
    val error: ComicAppError?,
  ) : com.hoc.comicapp.base.MviViewState {
    companion object {
      fun initial(): ViewState {
        return ViewState(
          user = null,
          isLoading = true,
          error = null
        )
      }
    }

    data class User(
      val uid: String,
      val displayName: String,
      val photoURL: String,
      val email: String,
    ) {
      constructor(domain: com.hoc.comicapp.domain.models.User) : this(
        uid = domain.uid,
        displayName = domain.displayName,
        photoURL = domain.photoURL,
        email = domain.email
      )
    }
  }

  sealed class PartialChange {
    fun reducer(vs: ViewState): ViewState {
      return when (this) {
        is User.UserChanged -> vs.copy(
          user = user,
          isLoading = false,
          error = null
        )
        is User.Error -> vs.copy(
          isLoading = false,
          error = error
        )
        User.Loading -> vs.copy(isLoading = true)
        SignOut.UserSignedOut -> vs.copy(
          user = null,
          isLoading = false,
          error = null
        )
        is SignOut.Error -> vs
      }
    }

    sealed class User : PartialChange() {
      object Loading : User()
      data class UserChanged(val user: ViewState.User?) : User()
      data class Error(val error: ComicAppError) : User()
    }

    sealed class SignOut : PartialChange() {
      object UserSignedOut : SignOut()
      data class Error(val error: ComicAppError) : SignOut()
    }
  }

  sealed class SingleEvent : com.hoc.comicapp.base.MviSingleEvent {
    data class GetUserError(val error: ComicAppError) : SingleEvent()
    object SignOutSuccess : SingleEvent()
    data class SignOutFailure(val error: ComicAppError) : SingleEvent()
  }

  interface Interactor {
    fun userChanges(): Observable<PartialChange>
    fun signOut(): Observable<PartialChange>
  }
}
