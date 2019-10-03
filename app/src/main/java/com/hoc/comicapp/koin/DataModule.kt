package com.hoc.comicapp.koin

import com.hoc.comicapp.data.ComicRepositoryImpl
import com.hoc.comicapp.data.DownloadComicsRepositoryImpl
import com.hoc.comicapp.data.local.AppDatabase
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

@ExperimentalCoroutinesApi
val dataModule = module {
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

  single { AppDatabase.getInstance(androidContext()) }

  single { get<AppDatabase>().chapterDao() }

  single { get<AppDatabase>().comicDao() }
}