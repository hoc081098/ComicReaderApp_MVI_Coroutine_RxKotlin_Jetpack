package com.hoc.comicapp.koin

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
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

private val provideAppCoroutineScope = { dispatchersProvider: CoroutinesDispatchersProvider ->
  CoroutineScope(dispatchersProvider.io + SupervisorJob())
}

private val provideMoshi = {
  Moshi
    .Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
}

val appModule = module {
  /*
   * Rx Schedulers + CoroutinesDispatchers + App CoroutineScope
   */

  singleOf(::RxSchedulerProviderImpl) { bind<RxSchedulerProvider>() }

  singleOf(::CoroutinesDispatchersProviderImpl) { bind<CoroutinesDispatchersProvider>() }

  singleOf(provideAppCoroutineScope)

  /*
   * WorkManager + Moshi + JsonAdaptersContainer
   */

  single { WorkManager.getInstance(androidContext()) }

  singleOf(provideMoshi)

  singleOf(::JsonAdaptersContainer)

  /*
   * FirebaseAuth + FirebaseStorage + FirebaseFirestore
   */

  single { FirebaseAuth.getInstance() }

  single { FirebaseStorage.getInstance() }

  single { FirebaseFirestore.getInstance() }
}
