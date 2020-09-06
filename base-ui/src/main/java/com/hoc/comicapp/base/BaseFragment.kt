package com.hoc.comicapp.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.koin.androidx.scope.ScopeFragment
import timber.log.Timber

abstract class BaseFragment<
  I : MviIntent,
  S : MviViewState,
  E : MviSingleEvent,
  VM : MviViewModel<I, S, E>>(@LayoutRes contentLayoutId: Int) :
  ScopeFragment(contentLayoutId),
  MviView<I, S, E> {
  protected val compositeDisposable = CompositeDisposable()

  protected abstract val viewModel: VM
  protected abstract val viewBinding: ViewBinding

  @CallSuper override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = super.onCreateView(inflater, container, savedInstanceState)!!
    .also { Timber.d("$this::onCreateView") }

  @CallSuper override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("$this::onViewCreated")

    setupView(view, savedInstanceState)
    bindVM()
  }

  @CallSuper override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("$this::onDestroyView")

    compositeDisposable.clear()
  }

  private fun bindVM() {
    viewModel.state.observe(owner = viewLifecycleOwner, ::render)
    viewModel.singleEvent.observeEvent(viewLifecycleOwner, ::handleEvent)
    viewModel.processIntents(viewIntents()).addTo(compositeDisposable)
  }

  /**
   * Call in onViewCreated
   */
  protected abstract fun setupView(view: View, savedInstanceState: Bundle?)
}
