package com.hoc.comicapp.koin

import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.JsonAdaptersContainer
import com.hoc.comicapp.data.analytics.AnalyticsServiceImpl
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSourceImpl
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSource
import com.hoc.comicapp.data.firebase.user.FirebaseAuthUserDataSourceImpl
import com.hoc.comicapp.data.local.AppDatabase
import com.hoc.comicapp.data.local.dao.ChapterDao
import com.hoc.comicapp.data.local.dao.ComicDao
import com.hoc.comicapp.data.remote.ComicApiService
import com.hoc.comicapp.data.repository.ComicRepositoryImpl
import com.hoc.comicapp.data.repository.DownloadComicsRepositoryImpl
import com.hoc.comicapp.data.repository.FavoriteComicsRepositoryImpl
import com.hoc.comicapp.data.repository.UserRepositoryImpl
import com.hoc.comicapp.domain.analytics.AnalyticsService
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.repository.UserRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

val dataModule = module {
  /*
   * FavoriteComicsRepository + ComicRepository + DownloadComicsRepository
   */

  single { provideFavoriteComicsRepository(get(), get()) }

  single {
    provideComicRepository(
      errorMapper = get(),
      comicApiService = get(),
      dispatchersProvider = get(),
      favoriteComicsDataSource = get(),
      comicDao = get(),
      appCoroutineScope = get(),
      analyticsService = get(),
    )
  }

  single {
    provideDownloadComicsRepository(
      application = androidApplication(),
      comicApiService = get(),
      dispatchersProvider = get(),
      comicDao = get(),
      chapterDao = get(),
      appDatabase = get(),
      rxSchedulerProvider = get(),
      errorMapper = get(),
      workManager = get(),
      jsonAdaptersContainer = get(),
      analyticsService = get(),
    )
  }

  single { provideUserRepository(get(), get()) }

  /*
   * AppDatabase + Dao
   */

  single { provideAppDatabase(androidContext()) }

  single { provideChapterDao(get()) }

  single { provideComicDao(get()) }

  /*
   * Firebase data source
   */

  single {
    provideFirebaseAuthUserDataSource(
      get(),
      get(),
      get(),
      get(),
      get(),
    )
  }

  single {
    provideFavoriteComicsDataSource(
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
    )
  }

  /*
   * ErrorMapper
   */

  single { provideErrorMapper(get()) }

  /*
   * Analytics
   */
  single<AnalyticsService> { AnalyticsServiceImpl() }
}

private fun provideFavoriteComicsRepository(
  errorMapper: ErrorMapper,
  favoriteComicsDataSource: FavoriteComicsDataSource,
): FavoriteComicsRepository {
  return FavoriteComicsRepositoryImpl(
    errorMapper = errorMapper,
    favoriteComicsDataSource = favoriteComicsDataSource,
  )
}

private fun provideComicRepository(
  errorMapper: ErrorMapper,
  comicApiService: ComicApiService,
  dispatchersProvider: CoroutinesDispatchersProvider,
  favoriteComicsDataSource: FavoriteComicsDataSource,
  comicDao: ComicDao,
  appCoroutineScope: CoroutineScope,
  analyticsService: AnalyticsService,
): ComicRepository {
  return ComicRepositoryImpl(
    errorMapper = errorMapper,
    comicApiService = comicApiService,
    dispatchersProvider = dispatchersProvider,
    favoriteComicsDataSource = favoriteComicsDataSource,
    comicDao = comicDao,
    appCoroutineScope = appCoroutineScope,
    analyticsService = analyticsService
  )
}

private fun provideDownloadComicsRepository(
  application: Application,
  comicApiService: ComicApiService,
  dispatchersProvider: CoroutinesDispatchersProvider,
  comicDao: ComicDao,
  chapterDao: ChapterDao,
  appDatabase: AppDatabase,
  rxSchedulerProvider: RxSchedulerProvider,
  errorMapper: ErrorMapper,
  workManager: WorkManager,
  jsonAdaptersContainer: JsonAdaptersContainer,
  analyticsService: AnalyticsService,
): DownloadComicsRepository {
  return DownloadComicsRepositoryImpl(
    comicApiService = comicApiService,
    application = application,
    dispatchersProvider = dispatchersProvider,
    comicDao = comicDao,
    chapterDao = chapterDao,
    appDatabase = appDatabase,
    rxSchedulerProvider = rxSchedulerProvider,
    errorMapper = errorMapper,
    workManager = workManager,
    jsonAdaptersContainer = jsonAdaptersContainer,
    analyticsService = analyticsService,
  )
}

private fun provideUserRepository(
  errorMapper: ErrorMapper,
  userDataSource: FirebaseAuthUserDataSource,
): UserRepository {
  return UserRepositoryImpl(
    errorMapper = errorMapper,
    userDataSource = userDataSource,
  )
}

private fun provideAppDatabase(context: Context): AppDatabase {
  return AppDatabase.getInstance(context)
}

private fun provideChapterDao(appDatabase: AppDatabase): ChapterDao {
  return appDatabase.chapterDao()
}

private fun provideComicDao(appDatabase: AppDatabase): ComicDao {
  return appDatabase.comicDao()
}

private fun provideFirebaseAuthUserDataSource(
  firebaseAuth: FirebaseAuth,
  firebaseStorage: FirebaseStorage,
  firebaseFirestore: FirebaseFirestore,
  rxSchedulerProvider: RxSchedulerProvider,
  dispatchersProvider: CoroutinesDispatchersProvider,
): FirebaseAuthUserDataSource {
  return FirebaseAuthUserDataSourceImpl(
    firebaseAuth = firebaseAuth,
    firebaseStorage = firebaseStorage,
    firebaseFirestore = firebaseFirestore,
    rxSchedulerProvider = rxSchedulerProvider,
    dispatchersProvider = dispatchersProvider,
  )
}

private fun provideFavoriteComicsDataSource(
  firebaseAuth: FirebaseAuth,
  firebaseFirestore: FirebaseFirestore,
  rxSchedulerProvider: RxSchedulerProvider,
  dispatchersProvider: CoroutinesDispatchersProvider,
  firebaseAuthUserDataSource: FirebaseAuthUserDataSource,
  appCoroutineScope: CoroutineScope,
): FavoriteComicsDataSource {
  return FavoriteComicsDataSourceImpl(
    firebaseAuth = firebaseAuth,
    firebaseFirestore = firebaseFirestore,
    rxSchedulerProvider = rxSchedulerProvider,
    dispatchersProvider = dispatchersProvider,
    firebaseAuthUserDataSource = firebaseAuthUserDataSource,
    appCoroutineScope = appCoroutineScope,
  )
}

private fun provideErrorMapper(retrofit: Retrofit): ErrorMapper {
  return ErrorMapper(retrofit)
}
