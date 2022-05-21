package com.hoc.comicapp.koin

import com.hoc.comicapp.activity.main.MainContract
import com.hoc.comicapp.activity.main.MainInteractorImpl
import com.hoc.comicapp.activity.main.MainVM
import com.hoc.comicapp.ui.category.CategoryFragment
import com.hoc.comicapp.ui.category.CategoryInteractor
import com.hoc.comicapp.ui.category.CategoryInteractorImpl
import com.hoc.comicapp.ui.category.CategoryViewModel
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract
import com.hoc.comicapp.ui.category_detail.CategoryDetailFragment
import com.hoc.comicapp.ui.category_detail.CategoryDetailInteractorImpl
import com.hoc.comicapp.ui.category_detail.CategoryDetailVM
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailFragment
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailInteractorImpl
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewModel
import com.hoc.comicapp.ui.detail.ComicDetailFragment
import com.hoc.comicapp.ui.detail.ComicDetailInteractor
import com.hoc.comicapp.ui.detail.ComicDetailInteractorImpl
import com.hoc.comicapp.ui.detail.ComicDetailViewModel
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsFragment
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsInteractorImpl
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsViewModel
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersFragment
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersViewModel
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsFragment
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsInteractorImpl
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsVM
import com.hoc.comicapp.ui.home.HomeFragment
import com.hoc.comicapp.ui.home.HomeInteractor
import com.hoc.comicapp.ui.home.HomeInteractorImpl
import com.hoc.comicapp.ui.home.HomeViewModel
import com.hoc.comicapp.ui.login.LoginContract
import com.hoc.comicapp.ui.login.LoginFragment
import com.hoc.comicapp.ui.login.LoginInteractorImpl
import com.hoc.comicapp.ui.login.LoginVM
import com.hoc.comicapp.ui.register.RegisterContract
import com.hoc.comicapp.ui.register.RegisterFragment
import com.hoc.comicapp.ui.register.RegisterInteractorImpl
import com.hoc.comicapp.ui.register.RegisterVM
import com.hoc.comicapp.ui.search_comic.SearchComicContract
import com.hoc.comicapp.ui.search_comic.SearchComicFragment
import com.hoc.comicapp.ui.search_comic.SearchComicInteractorImpl
import com.hoc.comicapp.ui.search_comic.SearchComicViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.scopedOf
import org.koin.dsl.module

val viewModelModule = module {
  scope<HomeFragment> {
    scopedOf(::HomeInteractorImpl) { bind<HomeInteractor>() }

    viewModelOf(::HomeViewModel)
  }

  scope<ComicDetailFragment> {
    scopedOf(::ComicDetailInteractorImpl) { bind<ComicDetailInteractor>() }

    viewModelOf(::ComicDetailViewModel)
  }

  scope<SearchComicFragment> {
    scopedOf(::SearchComicInteractorImpl) { bind<SearchComicContract.Interactor>() }

    viewModelOf(::SearchComicViewModel)
  }

  scope<CategoryFragment> {
    scopedOf(::CategoryInteractorImpl) { bind<CategoryInteractor>() }

    viewModelOf(::CategoryViewModel)
  }

  scope<ChapterDetailFragment> {
    scopedOf(::ChapterDetailInteractorImpl) { bind<ChapterDetailContract.Interactor>() }

    viewModelOf(::ChapterDetailViewModel)
  }

  scope<DownloadedComicsFragment> {
    scopedOf(::DownloadedComicsInteractorImpl) { bind<DownloadedComicsContract.Interactor>() }

    viewModelOf(::DownloadedComicsViewModel)
  }

  scope<CategoryDetailFragment> {
    scopedOf(::CategoryDetailInteractorImpl) { bind<CategoryDetailContract.Interactor>() }

    viewModelOf(::CategoryDetailVM)
  }

  scope<LoginFragment> {
    scopedOf(::LoginInteractorImpl) { bind<LoginContract.Interactor>() }

    viewModelOf(::LoginVM)
  }

  scope<RegisterFragment> {
    scopedOf(::RegisterInteractorImpl) { bind<RegisterContract.Interactor>() }

    viewModelOf(::RegisterVM)
  }

  scope<FavoriteComicsFragment> {
    scopedOf(::FavoriteComicsInteractorImpl) { bind<FavoriteComicsContract.Interactor>() }

    viewModelOf(::FavoriteComicsVM)
  }

  //region MainActivity
  factoryOf(::MainInteractorImpl) { bind<MainContract.Interactor>() }

  viewModelOf(::MainVM)
  //endregion

  scope<DownloadingChaptersFragment> {
    viewModelOf(::DownloadingChaptersViewModel)
  }
}
