package com.hoc.comicapp.ui.register

import android.app.Activity
import android.content.Intent.ACTION_GET_CONTENT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.register.RegisterContract.Intent
import com.hoc.comicapp.ui.register.RegisterContract.SingleEvent
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.getOrNull
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.onDismissed
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.toOptional
import com.hoc.comicapp.utils.uriFromResourceId
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.textChanges
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_register.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import rx_activity_result2.RxActivityResult
import android.content.Intent as AndroidIntent

class RegisterFragment : Fragment() {

  private val vm by viewModel<RegisterVM>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(R.layout.fragment_register, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    bindVM()

    image_avatar
      .clicks()
      .exhaustMap {
        val intent = AndroidIntent(ACTION_GET_CONTENT)
          .apply { type = "image/*" }
          .let { AndroidIntent.createChooser(it, "Choose avatar") }

        RxActivityResult
          .on(this@RegisterFragment)
          .startIntent(intent)
          .map {
            if (it.resultCode() == Activity.RESULT_OK) {
              it.data()?.data
            } else {
              null
            }.toOptional()
          }
      }
      .startWith(requireContext().uriFromResourceId(R.drawable.ic_person_white_24dp).toOptional())
      .subscribeBy {
        GlideApp
          .with(this@RegisterFragment)
          .load(it.getOrNull())
          .into(image_avatar)
      }
      .addTo(compositeDisposable)
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
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }

  private companion object {
    const val ANIM_DURATION = 300L
  }
}

