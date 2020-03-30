package com.hoc.comicapp.koin

import com.hoc.comicapp.activity.main.MainContract
import com.hoc.comicapp.activity.main.MainInteractorImpl
import com.hoc.comicapp.activity.main.MainVM
import com.hoc.comicapp.ui.category.CategoryInteractor
import com.hoc.comicapp.ui.category.CategoryInteractorImpl
import com.hoc.comicapp.ui.category.CategoryViewModel
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract
import com.hoc.comicapp.ui.category_detail.CategoryDetailInteractorImpl
import com.hoc.comicapp.ui.category_detail.CategoryDetailVM
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailInteractorImpl
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewModel
import com.hoc.comicapp.ui.detail.ComicDetailFragment
import com.hoc.comicapp.ui.detail.ComicDetailInteractor
import com.hoc.comicapp.ui.detail.ComicDetailInteractorImpl
import com.hoc.comicapp.ui.detail.ComicDetailViewModel
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsInteractorImpl
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsViewModel
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersViewModel
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsInteractorImpl
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsVM
import com.hoc.comicapp.ui.home.HomeInteractor
import com.hoc.comicapp.ui.home.HomeInteractorImpl
import com.hoc.comicapp.ui.home.HomeInteractorImpl1
import com.hoc.comicapp.ui.home.HomeViewModel
import com.hoc.comicapp.ui.login.LoginContract
import com.hoc.comicapp.ui.login.LoginInteractorImpl
import com.hoc.comicapp.ui.login.LoginVM
import com.hoc.comicapp.ui.register.RegisterContract
import com.hoc.comicapp.ui.register.RegisterInteractorImpl
import com.hoc.comicapp.ui.register.RegisterVM
import com.hoc.comicapp.ui.search_comic.SearchComicContract
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

  scope<ComicDetailFragment> {
    scoped<ComicDetailInteractor> {
      ComicDetailInteractorImpl(
        get(),
        get(),
        get(),
        get(),
        get()
      )
    }
    viewModel { (isDownloaded: Boolean) ->
      ComicDetailViewModel(
        get(),
        get(),
        get(),
        get(),
        isDownloaded
      )
    }
  }

  single { SearchComicInteractorImpl(get(), get()) } bind SearchComicContract.Interactor::class

  single { CategoryInteractorImpl(get(), get()) } bind CategoryInteractor::class

  single {
    ChapterDetailInteractorImpl(
      get(),
      get(),
      get()
    )
  } bind ChapterDetailContract.Interactor::class

  single {
    DownloadedComicsInteractorImpl(
      get(),
      androidApplication()
    )
  } bind DownloadedComicsContract.Interactor::class

  single {
    CategoryDetailInteractorImpl(
      get(),
      get()
    )
  } bind CategoryDetailContract.Interactor::class

  single { LoginInteractorImpl(get(), get()) } bind LoginContract.Interactor::class

  single { RegisterInteractorImpl(get(), get()) } bind RegisterContract.Interactor::class

  single { MainInteractorImpl(get(), get(), get()) } bind MainContract.Interactor::class

  single {
    FavoriteComicsInteractorImpl(
      get(),
      get(),
      get()
    )
  } bind FavoriteComicsContract.Interactor::class

  viewModel { HomeViewModel(get(), get()) }


  viewModel { SearchComicViewModel(get(), get()) }

  viewModel { CategoryViewModel(get(), get()) }

  viewModel { (isDownloaded: Boolean) -> ChapterDetailViewModel(get(), get(), isDownloaded) }

  viewModel { DownloadedComicsViewModel(get(), get()) }

  viewModel { DownloadingChaptersViewModel(get(), get(), get(), get()) }

  viewModel { (category: CategoryDetailContract.CategoryArg) ->
    CategoryDetailVM(
      get(),
      get(),
      category
    )
  }

  viewModel { LoginVM(get(), get()) }

  viewModel { RegisterVM(get(), get()) }

  viewModel { MainVM(get(), get()) }

  viewModel { FavoriteComicsVM(get(), get()) }
}