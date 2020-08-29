package com.hoc.comicapp.utils

import androidx.lifecycle.ViewModel
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ViewModelOwner
import org.koin.androidx.viewmodel.ViewModelOwnerDefinition
import org.koin.androidx.viewmodel.scope.BundleDefinition
import org.koin.androidx.viewmodel.scope.getViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

// TODO(Koin): Wait for fixing.
/**
 * ViewModel extensions to help for ViewModel
 */
inline fun <reified T : ViewModel> ScopeFragment.viewModel(
  qualifier: Qualifier? = null,
  noinline state: BundleDefinition? = null,
  noinline owner: ViewModelOwnerDefinition = { ViewModelOwner.from(this, this) },
  noinline parameters: ParametersDefinition? = null,
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
  scope.getViewModel(qualifier,
    state,
    owner,
    T::class,
    parameters)
}
