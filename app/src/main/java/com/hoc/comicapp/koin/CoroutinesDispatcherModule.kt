package com.hoc.comicapp.koin

import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.CoroutinesDispatcherProviderImpl
import org.koin.dsl.bind
import org.koin.dsl.module

val coroutinesDispatcherModule = module {
  single { CoroutinesDispatcherProviderImpl() } bind CoroutinesDispatcherProvider::class
}