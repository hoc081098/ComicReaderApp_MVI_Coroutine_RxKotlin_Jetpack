package com.hoc.comicapp.koin

import com.hoc.comicapp.CoroutinesDispatcherProviderImpl
import com.hoc.comicapp.RxSchedulerProviderImpl
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import org.koin.dsl.bind
import org.koin.dsl.module

val coroutinesDispatcherModule = module {
  single { RxSchedulerProviderImpl() } bind RxSchedulerProvider::class

  single { CoroutinesDispatcherProviderImpl(get()) } bind CoroutinesDispatcherProvider::class
}