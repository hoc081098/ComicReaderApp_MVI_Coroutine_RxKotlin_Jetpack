package com.hoc.comicapp.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.login.LoginContract.Intent
import com.hoc.comicapp.utils.observe
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

  private val vm by viewModel<LoginVM>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(R.layout.fragment_login, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    bindVM()
  }

  private fun bindVM() {
    vm.state.observe(owner = viewLifecycleOwner) {
      if (edit_email.error != it.emailError) {
        edit_email.error = it.emailError
      }
      if (edit_password.error != it.passwordError) {
        edit_password.error = it.passwordError
      }
    }

    vm.processIntents(
      Observable.mergeArray(
        edit_email.editText!!.textChanges().map { Intent.EmailChanged(it.toString()) },
        edit_password.editText!!.textChanges().map { Intent.PasswordChange(it.toString()) },
        button_login.clicks().map { Intent.SubmitLogin }
      )
    )
  }
}