package com.hoc.comicapp.ui.login

import com.hoc.comicapp.domain.models.ComicAppError
import io.reactivex.Observable

interface LoginContract {
  data class ViewState(
    val emailError: String?,
    val passwordError: String?,
    val isLoading: Boolean
  ) : com.hoc.comicapp.base.ViewState {
    companion object {
      @JvmStatic
      fun initial() = ViewState(
        isLoading = false,
        emailError = null,
        passwordError = null
      )
    }
  }

  sealed class Intent : com.hoc.comicapp.base.Intent {
    data class EmailChanged(val email: String) : Intent()
    data class PasswordChange(val password: String) : Intent()
    object SubmitLogin : Intent()
  }

  sealed class SingleEvent : com.hoc.comicapp.base.SingleEvent {
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
      }
    }

    data class EmailError(val error: String?) : PartialChange()
    data class PasswordError(val error: String?) : PartialChange()
    object Loading : PartialChange()
    object LoginSuccess : PartialChange()
    data class LoginFailure(val error: ComicAppError) : PartialChange()
  }

  interface Interactor {
    fun login(email: String, password: String): Observable<PartialChange>
  }
}