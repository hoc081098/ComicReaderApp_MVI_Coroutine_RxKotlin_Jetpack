package com.hoc.comicapp.koin

import androidx.work.WorkManager
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProviderImpl
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProviderImpl
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
  single { RxSchedulerProviderImpl() } bind RxSchedulerProvider::class

  single { CoroutinesDispatcherProviderImpl(get()) } bind CoroutinesDispatcherProvider::class

  single { WorkManager.getInstance(androidContext()) }

  single { getMoshi() }
}

private fun getMoshi(): Moshi {
  return Moshi
    .Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
}