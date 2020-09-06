package com.hoc.comicapp.ui.login

import com.hoc.comicapp.domain.models.ComicAppError
import io.reactivex.rxjava3.core.Observable

interface LoginContract {
  data class ViewState(
    val emailError: String?,
    val passwordError: String?,
    val isLoading: Boolean,
    // keep latest state when replace fragment
    val email: String?,
    val password: String?,
  ) : com.hoc.comicapp.base.MviViewState {
    companion object {
      @JvmStatic
      fun initial() = ViewState(
        isLoading = false,
        emailError = null,
        passwordError = null,
        email = null,
        password = null
      )
    }
  }

  sealed class Intent : com.hoc.comicapp.base.MviIntent {
    data class EmailChanged(val email: String) : Intent()
    data class PasswordChange(val password: String) : Intent()
    object SubmitLogin : Intent()
  }

  sealed class SingleEvent : com.hoc.comicapp.base.MviSingleEvent {
    object LoginSuccess : SingleEvent()
    data class LoginFailure(val error: ComicAppError) : SingleEvent()
  }

  sealed class PartialChange {
    fun reducer(state: ViewState): ViewState {
      return when (this) {
        is EmailError -> state.copy(emailError = error)
        is PasswordError -> state.copy(passwordError = error)
        Loading -> state.copy(isLoading = true)
        LoginSuccess -> state.copy(isLoading = false)
        is LoginFailure -> state.copy(isLoading = false)
        is EmailChanged -> state.copy(email = email)
        is PasswordChanged -> state.copy(password = password)
      }
    }

    data class EmailError(val error: String?) : PartialChange()
    data class PasswordError(val error: String?) : PartialChange()

    data class EmailChanged(val email: String) : PartialChange()
    data class PasswordChanged(val password: String) : PartialChange()

    object Loading : PartialChange()
    object LoginSuccess : PartialChange()
    data class LoginFailure(val error: ComicAppError) : PartialChange()
  }

  interface Interactor {
    fun login(email: String, password: String): Observable<PartialChange>
  }
}
