package com.hoc.comicapp.koin

import com.hoc.comicapp.ui.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

@ExperimentalCoroutinesApi
val viewModelModule = module {
  viewModel { HomeViewModel(get(), get())  }
}