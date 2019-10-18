package com.hoc.comicapp.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.register.RegisterContract.Intent
import com.hoc.comicapp.ui.register.RegisterContract.SingleEvent
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
    vm.state.observe(owner = viewLifecycleOwner) { (emailError, passwordError, fullNameError, isLoading) ->
      if (edit_email.error != emailError) {
        edit_email.error = emailError
      }
      if (edit_password.error != passwordError) {
        edit_password.error = passwordError
      }
      if (edit_full_name.error != fullNameError) {
        edit_full_name.error = fullNameError
      }
    }

    vm.singleEvent.observeEvent(owner = viewLifecycleOwner) { event ->
      when (event) {
        SingleEvent.RegisterSuccess -> {
          view?.snack("Register success") {
            onDismissed {
              findNavController().popBackStack(R.id.home_fragment_dest, false)
            }
          }
        }
        is SingleEvent.RegisterFailure -> {
          view?.snack("Register error: ${event.error.getMessage()}")
        }
      }
    }

    vm.processIntents(
      Observable.mergeArray(
        edit_email.editText!!.textChanges().map { Intent.EmailChanged(it.toString()) },
        edit_password.editText!!.textChanges().map { Intent.PasswordChanged(it.toString()) },
        edit_full_name.editText!!.textChanges().map { Intent.FullNameChanged(it.toString()) },
        button_register.clicks().map { Intent.SubmitRegister }
      )
    )
  }

  private companion object {
    const val ANIM_DURATION = 300L
  }
}

