package com.hoc.comicapp.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.login.LoginContract.Intent
import com.hoc.comicapp.ui.login.LoginContract.SingleEvent
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.onDismissed
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_register.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class RegisterFragment : Fragment() {

  private val vm by viewModel<RegisterVM>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(R.layout.fragment_register, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    bindVM()
  }

  private fun bindVM() {
    vm.state.observe(owner = viewLifecycleOwner) { (emailError, passwordError, isLoading) ->
      if (edit_email.error != emailError) {
        edit_email.error = emailError
      }
      if (edit_password.error != passwordError) {
        edit_password.error = passwordError
      }


    }

    vm.singleEvent.observeEvent(owner = viewLifecycleOwner) { event ->
      when (event) {
        SingleEvent.LoginSuccess -> {
          view?.snack("Login success") {
            onDismissed {
              findNavController().popBackStack()
            }
          }
        }
        is SingleEvent.LoginFailure -> {
          view?.snack("Login error: ${event.error.getMessage()}")
        }
      }
    }

    vm.processIntents(
      Observable.mergeArray(
        edit_email.editText!!.textChanges().map { Intent.EmailChanged(it.toString()) },
        edit_password.editText!!.textChanges().map { Intent.PasswordChange(it.toString()) },
        button_register.clicks().map { Intent.SubmitLogin }
      )
    )
  }


  private companion object {
    const val ANIM_DURATION = 300L
  }
}

