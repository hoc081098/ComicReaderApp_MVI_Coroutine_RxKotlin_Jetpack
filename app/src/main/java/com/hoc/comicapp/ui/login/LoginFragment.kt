package com.hoc.comicapp.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_login.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginFragment : Fragment() {

  private val vm by viewModel<LoginVM>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(R.layout.fragment_login, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    button_register.setOnClickListener {
      val toRegisterFragment =
        LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
      findNavController().navigate(toRegisterFragment)
    }

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

      if (isLoading) {
        beginTransition(button_login, progress_bar)
      } else {
        onComplete(button_login, progress_bar)
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
        button_login.clicks().map { Intent.SubmitLogin }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }

  private fun beginTransition(
    button: Button,
    progressBar: ProgressBar
  ) {
    TransitionManager.beginDelayedTransition(
      root_login_frag,
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
    progressBar: ProgressBar
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

    TransitionManager.beginDelayedTransition(root_login_frag, transition)

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

