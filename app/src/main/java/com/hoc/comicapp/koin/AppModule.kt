package com.hoc.comicapp.koin

import androidx.work.WorkManager
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProviderImpl
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProviderImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
  single { RxSchedulerProviderImpl() } bind RxSchedulerProvider::class

  single { CoroutinesDispatcherProviderImpl(get()) } bind CoroutinesDispatcherProvider::class

  single { WorkManager.getInstance(androidContext()) }
}