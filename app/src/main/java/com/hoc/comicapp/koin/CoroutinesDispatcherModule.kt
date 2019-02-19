package com.hoc.comicapp.koin

import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.CoroutinesDispatcherProviderImpl
import org.koin.dsl.module.module

val coroutinesDispatcherModule = module {
  single { CoroutinesDispatcherProviderImpl() } bind CoroutinesDispatcherProvider::class
}