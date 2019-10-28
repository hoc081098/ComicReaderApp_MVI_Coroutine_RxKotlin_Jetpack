package com.hoc.comicapp.koin

import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSource
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSourceImpl
import com.hoc.comicapp.data.local.AppDatabase
import com.hoc.comicapp.data.repository.ComicRepositoryImpl
import com.hoc.comicapp.data.repository.DownloadComicsRepositoryImpl
import com.hoc.comicapp.data.repository.FavoriteComicsRepositoryImpl
import com.hoc.comicapp.data.repository.UserRepositoryImpl
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

@ExperimentalCoroutinesApi
val dataModule = module {
  single { FavoriteComicsRepositoryImpl(get(), get(), get(), get(), get()) } bind FavoriteComicsRepository::class

  single { ComicRepositoryImpl(get(), get(), get()) } bind ComicRepository::class

  single {
    DownloadComicsRepositoryImpl(
      get(),
      androidApplication(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get()
    )
  } bind DownloadComicsRepository::class

  single { UserRepositoryImpl(get(), get()) } bind UserRepository::class

  single { AppDatabase.getInstance(androidContext()) }

  single { get<AppDatabase>().chapterDao() }

  single { get<AppDatabase>().comicDao() }

  single {
    FirebaseAuthUserDataSourceImpl(
      get(),
      get(),
      get(),
      get(),
      get()
    )
  } bind FirebaseAuthUserDataSource::class
}