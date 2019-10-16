package com.hoc.comicapp.ui.login

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
  }

  sealed class SingleEvent : com.hoc.comicapp.base.SingleEvent {

  }

  sealed class PartialChange {
    abstract fun reducer(state: ViewState): ViewState

    data class EmailError(val error: String?) : PartialChange() {
      override fun reducer(state: ViewState): ViewState {
        return state.copy(emailError = error)
      }
    }

    data class PasswordError(val error: String?) : PartialChange() {
      override fun reducer(state: ViewState): ViewState {
        return state.copy(passwordError = error)
      }
    }
  }
}