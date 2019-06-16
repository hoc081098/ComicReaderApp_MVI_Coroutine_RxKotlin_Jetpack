package com.hoc.comicapp.koin

import com.hoc.comicapp.ui.category.CategoryInteractor
import com.hoc.comicapp.ui.category.CategoryInteractorImpl
import com.hoc.comicapp.ui.category.CategoryViewModel
import com.hoc.comicapp.ui.detail.ComicDetailInteractor
import com.hoc.comicapp.ui.detail.ComicDetailInteractorImpl
import com.hoc.comicapp.ui.detail.ComicDetailViewModel
import com.hoc.comicapp.ui.home.HomeInteractor
import com.hoc.comicapp.ui.home.HomeInteractorImpl
import com.hoc.comicapp.ui.home.HomeViewModel
import com.hoc.comicapp.ui.search_comic.SearchComicInteractor
import com.hoc.comicapp.ui.search_comic.SearchComicInteractorImpl
import com.hoc.comicapp.ui.search_comic.SearchComicViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

@ExperimentalCoroutinesApi
val viewModelModule = module {

  single { HomeInteractorImpl(get()) } bind HomeInteractor::class

  single { ComicDetailInteractorImpl(get()) } bind ComicDetailInteractor::class

  single { SearchComicInteractorImpl(get()) } bind SearchComicInteractor::class

  single { CategoryInteractorImpl(get()) } bind CategoryInteractor::class

  viewModel { HomeViewModel(get()) }

  viewModel { ComicDetailViewModel(get()) }

  viewModel { SearchComicViewModel(get()) }

  viewModel { CategoryViewModel(get()) }

}