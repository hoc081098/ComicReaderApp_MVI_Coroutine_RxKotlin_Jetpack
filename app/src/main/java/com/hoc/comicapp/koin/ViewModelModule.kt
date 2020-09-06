package com.hoc.comicapp.koin

import com.hoc.comicapp.activity.main.MainActivity
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
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
  scope<HomeFragment> {
    scoped<HomeInteractor> {
      HomeInteractorImpl(
        comicRepository = get(),
        dispatchersProvider = get(),
      )
    }

    viewModel {
      HomeViewModel(
        homeInteractor = get(),
        rxSchedulerProvider = get(),
      )
    }
  }

  scope<ComicDetailFragment> {
    scoped<ComicDetailInteractor> {
      ComicDetailInteractorImpl(
        comicRepository = get(),
        dispatchersProvider = get(),
        downloadedComicRepository = get(),
        favoriteComicsRepository = get(),
        rxSchedulerProvider = get(),
      )
    }

    viewModel { (isDownloaded: Boolean) ->
      ComicDetailViewModel(
        comicDetailInteractor = get(),
        downloadComicsRepository = get(),
        rxSchedulerProvider = get(),
        workManager = get(),
        isDownloaded = isDownloaded,
      )
    }
  }

  scope<SearchComicFragment> {
    scoped<SearchComicContract.Interactor> {
      SearchComicInteractorImpl(
        comicRepository = get(),
        dispatchersProvider = get(),
      )
    }

    viewModel {
      SearchComicViewModel(
        interactor = get(),
        rxSchedulerProvider = get(),
      )
    }
  }

  scope<CategoryFragment> {
    scoped<CategoryInteractor> {
      CategoryInteractorImpl(
        comicRepository = get(),
        dispatchersProvider = get(),
      )
    }

    viewModel {
      CategoryViewModel(
        categoryInteractor = get(),
        rxSchedulerProvider = get(),
      )
    }
  }

  scope<ChapterDetailFragment> {
    scoped<ChapterDetailContract.Interactor> {
      ChapterDetailInteractorImpl(
        comicRepository = get(),
        dispatchersProvider = get(),
        downloadComicsRepository = get(),
      )
    }

    viewModel { (isDownloaded: Boolean) ->
      ChapterDetailViewModel(
        interactor = get(),
        rxSchedulerProvider = get(),
        isDownloaded = isDownloaded,
      )
    }
  }

  scope<DownloadedComicsFragment> {
    scoped<DownloadedComicsContract.Interactor> {
      DownloadedComicsInteractorImpl(
        downloadComicsRepository = get(),
        application = androidApplication(),
      )
    }

    viewModel {
      DownloadedComicsViewModel(
        rxSchedulerProvider = get(),
        interactor = get(),
      )
    }
  }

  scope<CategoryDetailFragment> {
    scoped<CategoryDetailContract.Interactor> {
      CategoryDetailInteractorImpl(
        dispatchersProvider = get(),
        comicRepository = get(),
      )
    }

    viewModel { (category: CategoryDetailContract.CategoryArg) ->
      CategoryDetailVM(
        rxSchedulerProvider = get(),
        interactor = get(),
        category = category,
      )
    }
  }

  scope<LoginFragment> {
    scoped<LoginContract.Interactor> {
      LoginInteractorImpl(
        userRepository = get(),
        dispatchersProvider = get(),
      )
    }

    viewModel {
      LoginVM(
        interactor = get(),
        rxSchedulerProvider = get(),
      )
    }
  }

  scope<RegisterFragment> {
    scoped<RegisterContract.Interactor> {
      RegisterInteractorImpl(
        userRepository = get(),
        dispatchersProvider = get(),
      )
    }

    viewModel {
      RegisterVM(
        interactor = get(),
        rxSchedulerProvider = get(),
      )
    }
  }

  scope<FavoriteComicsFragment> {
    scoped<FavoriteComicsContract.Interactor> {
      FavoriteComicsInteractorImpl(
        favoriteComicsRepository = get(),
        rxSchedulerProvider = get(),
        dispatchersProvider = get(),
      )
    }

    viewModel {
      FavoriteComicsVM(
        interactor = get(),
        rxSchedulerProvider = get(),
      )
    }
  }

  scope<MainActivity> {
    scoped<MainContract.Interactor> {
      MainInteractorImpl(
        userRepository = get(),
        dispatchersProvider = get(),
        rxSchedulerProvider = get(),
      )
    }

    viewModel {
      MainVM(
        interactor = get(),
        rxSchedulerProvider = get(),
      )
    }
  }

  scope<DownloadingChaptersFragment> {
    viewModel {
      DownloadingChaptersViewModel(
        rxSchedulerProvider = get(),
        workManager = get(),
        jsonAdaptersContainer = get(),
        downloadComicsRepository = get(),
      )
    }
  }
}
