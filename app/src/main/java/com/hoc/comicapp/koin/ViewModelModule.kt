package com.hoc.comicapp.koin

import com.hoc.comicapp.ui.home.HomeInteractor
import com.hoc.comicapp.ui.home.HomeInteractorImpl
import com.hoc.comicapp.ui.home.HomeInteractorImpl1
import com.hoc.comicapp.ui.home.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

@ExperimentalCoroutinesApi
val viewModelModule = module {
  single { HomeInteractorImpl(get()) }

  single { HomeInteractorImpl1(get(), get()) } bind HomeInteractor::class

  viewModel { HomeViewModel(get()) }
}