package com.hoc.comicapp.koin

import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.analytics.AnalyticsServiceImpl
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSourceImpl
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSource
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSourceImpl
import com.hoc.comicapp.data.local.AppDatabase
import com.hoc.comicapp.data.repository.ComicRepositoryImpl
import com.hoc.comicapp.data.repository.DownloadComicsRepositoryImpl
import com.hoc.comicapp.data.repository.FavoriteComicsRepositoryImpl
import com.hoc.comicapp.data.repository.UserRepositoryImpl
import com.hoc.comicapp.domain.analytics.AnalyticsService
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.repository.UserRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
  /*
   * FavoriteComicsRepository + ComicRepository + DownloadComicsRepository
   */

  singleOf(::FavoriteComicsRepositoryImpl) { bind<FavoriteComicsRepository>() }

  singleOf(::ComicRepositoryImpl) { bind<ComicRepository>() }

  singleOf(::DownloadComicsRepositoryImpl) { bind<DownloadComicsRepository>() }

  singleOf(::UserRepositoryImpl) { bind<UserRepository>() }

  /*
   * AppDatabase + Dao
   */

  single { AppDatabase.getInstance(androidContext()) }

  single { get<AppDatabase>().chapterDao() }

  single { get<AppDatabase>().comicDao() }

  /*
   * Firebase data source
   */

  singleOf(::FirebaseAuthUserDataSourceImpl) { bind<FirebaseAuthUserDataSource>() }

  singleOf(::FavoriteComicsDataSourceImpl) { bind<FavoriteComicsDataSource>() }

  /*
   * ErrorMapper
   */

  singleOf(::ErrorMapper)

  /*
   * Analytics
   */
  singleOf(::AnalyticsServiceImpl) { bind<AnalyticsService>() }
}
