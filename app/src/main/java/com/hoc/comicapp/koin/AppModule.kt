package com.hoc.comicapp.koin

import android.content.Context
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hoc.comicapp.data.JsonAdaptersContainer
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProviderImpl
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProviderImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
  /*
   * Rx Schedulers + CoroutinesDispatchers + App CoroutineScope
   */

  single { provideRxSchedulerProvider() }

  single { provideCoroutinesDispatchersProvider(get()) }

  single { provideAppCoroutineScope(get()) }

  /*
   * WorkManager + Moshi + JsonAdaptersContainer
   */

  single { provideWorkManager(androidContext()) }

  single { provideMoshi() }

  single { provideJsonAdaptersContainer(get()) }

  /*
   * FirebaseAuth + FirebaseStorage + FirebaseFirestore
   */

  single { provideFirebaseAuth() }

  single { provideFirebaseStorage() }

  single { provideFirebaseFirestore() }
}

private fun provideAppCoroutineScope(dispatchersProvider: CoroutinesDispatchersProvider): CoroutineScope {
  return CoroutineScope(dispatchersProvider.io + SupervisorJob())
}

private fun provideFirebaseAuth(): FirebaseAuth {
  return FirebaseAuth.getInstance()
}

private fun provideFirebaseStorage(): FirebaseStorage {
  return FirebaseStorage.getInstance()
}

private fun provideFirebaseFirestore(): FirebaseFirestore {
  return FirebaseFirestore.getInstance()
}

private fun provideWorkManager(context: Context): WorkManager {
  return WorkManager.getInstance(context)
}

private fun provideRxSchedulerProvider(): RxSchedulerProvider {
  return RxSchedulerProviderImpl()
}

private fun provideCoroutinesDispatchersProvider(rxSchedulerProvider: RxSchedulerProvider): CoroutinesDispatchersProvider {
  return CoroutinesDispatchersProviderImpl(rxSchedulerProvider)
}

private fun provideJsonAdaptersContainer(moshi: Moshi): JsonAdaptersContainer {
  return JsonAdaptersContainer(moshi)
}

private fun provideMoshi(): Moshi {
  return Moshi
    .Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
}
