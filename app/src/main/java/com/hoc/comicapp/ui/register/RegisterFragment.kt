package com.hoc.comicapp.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.register.RegisterContract.Intent
import com.hoc.comicapp.ui.register.RegisterContract.SingleEvent
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.onDismissed
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.uriFromResourceId
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.widget.textChanges
import io.reactivex.rxjava3.android.MainThreadDisposable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.android.synthetic.main.fragment_register.*
import org.koin.androidx.scope.lifecycleScope
import org.koin.androidx.viewmodel.scope.viewModel
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class RegisterFragment : Fragment() {

  private val vm by lifecycleScope.viewModel<RegisterVM>(owner = this)
  private val compositeDisposable = CompositeDisposable()
  private val glide by lazy(NONE) { GlideApp.with(this) }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = inflater.inflate(R.layout.fragment_register, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    edit_full_name.editText!!.setText(vm.state.value.fullName ?: "")
    edit_email.editText!!.setText(vm.state.value.email ?: "")
    edit_password.editText!!.setText(vm.state.value.password ?: "")

    button_back_to_login.setOnClickListener { findNavController().popBackStack() }

    bindVM()
  }

  private fun bindVM() {
    vm.state.observe(owner = viewLifecycleOwner) { (emailError, passwordError, fullNameError, isLoading, avatarUri) ->
      if (edit_email.error != emailError) {
        edit_email.error = emailError
      }
      if (edit_password.error != passwordError) {
        edit_password.error = passwordError
      }
      if (edit_full_name.error != fullNameError) {
        edit_full_name.error = fullNameError
      }

      if (isLoading) {
        beginTransition(button_register, progress_bar)
      } else {
        onComplete(button_register, progress_bar)
      }

      glide
        .load(avatarUri ?: requireContext().uriFromResourceId(R.drawable.person_white_96x96))
        .centerCrop()
        .dontAnimate()
        .into(image_avatar)
    }

    vm.singleEvent.observeEvent(owner = viewLifecycleOwner) { event ->
      when (event) {
        SingleEvent.RegisterSuccess -> {
          view?.snack("Register success") {
            onDismissed {
              Timber.d("onDismissed")
              activity
                ?.findNavController(R.id.main_nav_fragment)
                ?.popBackStack(R.id.home_fragment_dest, false)
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
        changeAvatarIntent(),
        button_register.clicks().map { Intent.SubmitRegister }
      )
    ).addTo(compositeDisposable)
  }

  private fun changeAvatarIntent(): Observable<Intent.AvatarChanged> {
    return image_avatar
      .clicks()
      .exhaustMap {
        Timber.d("Select image")

        Observable.create<Intent.AvatarChanged> { emitter ->
          val launcher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { emitter.onNext(Intent.AvatarChanged(it)) }
            emitter.onComplete()
          }.apply { launch(arrayOf("image/*")) }

          emitter.setDisposable(object : MainThreadDisposable() {
            override fun onDispose() = launcher.unregister()
          })
        }
      }
      .doOnNext { Timber.d("Select image $it") }
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
      root_register_frag,
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

    TransitionManager.beginDelayedTransition(root_register_frag, transition)

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

