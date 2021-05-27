package com.hoc.comicapp.ui.register

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import arrow.core.Option
import arrow.core.Some
import arrow.core.toOption
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.FragmentRegisterBinding
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.koin.appNavigator
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.ui.register.RegisterContract.Intent
import com.hoc.comicapp.ui.register.RegisterContract.SingleEvent
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.onDismissed
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.uriFromResourceId
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.widget.textChanges
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class RegisterFragment : ScopeFragment() {

  private val vm by viewModel<RegisterVM>()
  private val viewBinding by viewBinding<FragmentRegisterBinding>()
  private val compositeDisposable = CompositeDisposable()
  private val glide by lazy(NONE) { GlideApp.with(this) }

  private val avatarUriS = PublishRelay.create<Option<Uri>>()
  private val openDocumentLauncher =
    registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
      avatarUriS.accept(uri.toOption())
    }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = inflater.inflate(R.layout.fragment_register, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewBinding.editFullName.editText!!.setText(vm.state.value.fullName ?: "")
    viewBinding.editEmail.editText!!.setText(vm.state.value.email ?: "")
    viewBinding.editPassword.editText!!.setText(vm.state.value.password ?: "")

    viewBinding.buttonBackToLogin.setOnClickListener {
      requireAppNavigator.execute { popBackStack() }
    }

    bindVM()
  }

  private fun bindVM() = viewBinding.run {
    vm.state.observe(owner = viewLifecycleOwner) { (emailError, passwordError, fullNameError, isLoading, avatarUri) ->
      if (editEmail.error != emailError) {
        editEmail.error = emailError
      }
      if (editPassword.error != passwordError) {
        editPassword.error = passwordError
      }
      if (editFullName.error != fullNameError) {
        editFullName.error = fullNameError
      }

      if (isLoading) {
        beginTransition(buttonRegister, progressBar)
      } else {
        onComplete(buttonRegister, progressBar)
      }

      glide
        .load(avatarUri ?: requireContext().uriFromResourceId(R.drawable.person_white_96x96))
        .centerCrop()
        .dontAnimate()
        .into(imageAvatar)
    }

    vm.singleEvent.observeEvent(owner = viewLifecycleOwner) { event ->
      when (event) {
        SingleEvent.RegisterSuccess -> {
          view?.snack("Register success") {
            onDismissed {
              Timber.d("onDismissed")
              appNavigator?.execute { popBackStack(R.id.home_fragment_dest, false) }
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
        editEmail.editText!!.textChanges().map { Intent.EmailChanged(it.toString()) },
        editPassword.editText!!.textChanges().map { Intent.PasswordChanged(it.toString()) },
        editFullName.editText!!.textChanges().map { Intent.FullNameChanged(it.toString()) },
        changeAvatarIntent(),
        buttonRegister.clicks().map { Intent.SubmitRegister }
      )
    ).addTo(compositeDisposable)
  }

  private fun changeAvatarIntent(): Observable<Intent.AvatarChanged> {
    return viewBinding.imageAvatar
      .clicks()
      .exhaustMap {
        avatarUriS
          .take(1)
          .doOnSubscribe {
            Timber.d("Start image")
            openDocumentLauncher.launch(arrayOf("image/*"))
          }
      }
      .doOnNext { Timber.d("Picked $it") }
      .ofType<Some<Uri>>()
      .map { Intent.AvatarChanged(it.value) }
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
      viewBinding.rootRegisterFrag,
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

    TransitionManager.beginDelayedTransition(viewBinding.rootRegisterFrag, transition)

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
