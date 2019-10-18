package com.hoc.comicapp.ui.register

import android.net.Uri
import com.hoc.comicapp.domain.models.ComicAppError
import io.reactivex.Observable

interface RegisterContract {
  data class ViewState(
    val emailError: String?,
    val passwordError: String?,
    val fullNameError: String?,
    val isLoading: Boolean,
    val avatar: Uri?
  ) : com.hoc.comicapp.base.ViewState {
    companion object {
      @JvmStatic
      fun initial() = ViewState(
        isLoading = false,
        emailError = null,
        passwordError = null,
        fullNameError = null,
        avatar = null
      )
    }
  }

  sealed class Intent : com.hoc.comicapp.base.Intent {
    data class AvatarChanged(val uri: Uri) : Intent()
    data class EmailChanged(val email: String) : Intent()
    data class PasswordChanged(val password: String) : Intent()
    data class FullNameChanged(val fullName: String) : Intent()
    object SubmitRegister : Intent()
  }

  sealed class SingleEvent : com.hoc.comicapp.base.SingleEvent {
    object RegisterSuccess : SingleEvent()
    data class RegisterFailure(val error: ComicAppError) : SingleEvent()
  }

  sealed class PartialChange {
    fun reducer(state: ViewState): ViewState {
      return when (this) {
        is EmailError -> state.copy(emailError = error)
        is PasswordError -> state.copy(passwordError = error)
        is FullNameError -> state.copy(fullNameError = error)
        is AvatarChanged -> state.copy(avatar = uri)
        Loading -> state.copy(isLoading = true)
        RegisterSuccess -> state.copy(isLoading = false)
        is RegisterFailure -> state.copy(isLoading = false)
      }
    }

    data class EmailError(val error: String?) : PartialChange()
    data class PasswordError(val error: String?) : PartialChange()
    data class FullNameError(val error: String?) : PartialChange()
    data class AvatarChanged(val uri: Uri) : PartialChange()

    object Loading : PartialChange()
    object RegisterSuccess : PartialChange()
    data class RegisterFailure(val error: ComicAppError) : PartialChange()
  }

  data class User(
    val email: String,
    val password: String,
    val fullName: String,
    val avatar: Uri?
  )

  interface Interactor {
    fun register(user: User): Observable<PartialChange>
  }
}