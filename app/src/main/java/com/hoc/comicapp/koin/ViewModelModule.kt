package com.hoc.comicapp.koin

import com.hoc.comicapp.ui.category.CategoryInteractor
import com.hoc.comicapp.ui.category.CategoryInteractorImpl
import com.hoc.comicapp.ui.category.CategoryViewModel
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailInteractor
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailInteractorImpl
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewModel
import com.hoc.comicapp.ui.detail.ComicDetailInteractor
import com.hoc.comicapp.ui.detail.ComicDetailInteractorImpl
import com.hoc.comicapp.ui.detail.ComicDetailViewModel
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsInteractorImpl
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsViewModel
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersViewModel
import com.hoc.comicapp.ui.home.HomeInteractor
import com.hoc.comicapp.ui.home.HomeInteractorImpl
import com.hoc.comicapp.ui.home.HomeInteractorImpl1
import com.hoc.comicapp.ui.home.HomeViewModel
import com.hoc.comicapp.ui.search_comic.SearchComicInteractor
import com.hoc.comicapp.ui.search_comic.SearchComicInteractorImpl
import com.hoc.comicapp.ui.search_comic.SearchComicViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

@ExperimentalCoroutinesApi
val viewModelModule = module {

  single { HomeInteractorImpl(get(), get()) }

  single { HomeInteractorImpl1(get(), get(), get()) } bind HomeInteractor::class

  single { ComicDetailInteractorImpl(get(), get()) } bind ComicDetailInteractor::class

  single { SearchComicInteractorImpl(get(), get()) } bind SearchComicInteractor::class

  single { CategoryInteractorImpl(get(), get()) } bind CategoryInteractor::class

  single { ChapterDetailInteractorImpl(get(), get()) } bind ChapterDetailInteractor::class

  single { DownloadedComicsInteractorImpl(get(), androidApplication()) } bind DownloadedComicsContract.Interactor::class

  viewModel { HomeViewModel(get(), get()) }

  viewModel { ComicDetailViewModel(get(), get(), get(), get(), get()) }

  viewModel { SearchComicViewModel(get(), get()) }

  viewModel { CategoryViewModel(get(), get()) }

  viewModel { ChapterDetailViewModel(get(), get()) }

  viewModel { DownloadedComicsViewModel(get(), get()) }

  viewModel { DownloadingChaptersViewModel(get(), get(), get()) }
}