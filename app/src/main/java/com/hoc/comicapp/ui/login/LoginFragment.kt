package com.hoc.comicapp.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.FragmentLoginBinding
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.koin.appNavigator
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.ui.login.LoginContract.Intent
import com.hoc.comicapp.ui.login.LoginContract.SingleEvent
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.onDismissed
import com.hoc.comicapp.utils.snack
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.widget.textChanges
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class LoginFragment : ScopeFragment() {

  private val vm by viewModel<LoginVM>()
  private val viewBinding by viewBinding<FragmentLoginBinding>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = inflater.inflate(R.layout.fragment_login, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewBinding.editEmail.editText!!.setText(vm.state.value.email ?: "")
    viewBinding.editPassword.editText!!.setText(vm.state.value.password ?: "")

    viewBinding.buttonRegister.setOnClickListener {
      val toRegisterFragment =
        LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
      requireAppNavigator.execute { navigate(toRegisterFragment) }
    }

    bindVM()
  }

  private fun bindVM() = viewBinding.run {
    vm.state.observe(owner = viewLifecycleOwner) { (emailError, passwordError, isLoading) ->
      if (editEmail.error != emailError) {
        editEmail.error = emailError
      }
      if (editPassword.error != passwordError) {
        editPassword.error = passwordError
      }

      if (isLoading) {
        beginTransition(buttonLogin, progressBar)
      } else {
        onComplete(buttonLogin, progressBar)
      }
    }

    vm.singleEvent.observeEvent(owner = viewLifecycleOwner) { event ->
      when (event) {
        SingleEvent.LoginSuccess -> {
          view?.snack("Login success") {
            onDismissed {
              Timber.d("onDismissed")
              appNavigator?.execute { popBackStack(R.id.home_fragment_dest, false) }
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
        editEmail.editText!!.textChanges().map { Intent.EmailChanged(it.toString()) },
        editPassword.editText!!.textChanges().map { Intent.PasswordChange(it.toString()) },
        buttonLogin.clicks().map { Intent.SubmitLogin }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }

  private fun beginTransition(
    button: Button,
    progressBar: ProgressBar,
  ) {
    TransitionManager.beginDelayedTransition(
      viewBinding.rootLoginFrag,
      TransitionSet()
        .addTransition(
          ChangeBounds()
            .addTarget(button)
            .setDuration(ANIM_DURATION)
            .setInterpolator(AccelerateDecelerateInterpolator())
        )
        .addTransition(
          Fade()
            .addTarget(button)
            .setDuration(ANIM_DURATION)
        )
        .addTransition(
          Fade().addTarget(progressBar)
            .setDuration(ANIM_DURATION)
        )
        .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
    )

    button.layoutParams = (button.layoutParams as ConstraintLayout.LayoutParams).apply {
      width = height
    }
    button.visibility = View.INVISIBLE
    progressBar.visibility = View.VISIBLE
  }

  private fun onComplete(
    button: Button,
    progressBar: ProgressBar,
  ) {
    val transition = TransitionSet()
      .addTransition(
        Fade().addTarget(progressBar)
          .setDuration(ANIM_DURATION)
      )
      .addTransition(
        Fade()
          .addTarget(button)
          .setDuration(ANIM_DURATION)
      )
      .addTransition(
        ChangeBounds()
          .addTarget(button)
          .setDuration(ANIM_DURATION)
          .setInterpolator(AccelerateDecelerateInterpolator())
      )
      .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)

    TransitionManager.beginDelayedTransition(viewBinding.rootLoginFrag, transition)

    progressBar.visibility = View.INVISIBLE
    button.visibility = View.VISIBLE

    button.layoutParams = (button.layoutParams as ConstraintLayout.LayoutParams).apply {
      width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
    }
  }

  private companion object {
    const val ANIM_DURATION = 300L
  }
}
