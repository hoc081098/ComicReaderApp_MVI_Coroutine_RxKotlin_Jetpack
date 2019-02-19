package com.hoc.comicapp

import android.app.Application
import com.hoc.comicapp.koin.coroutinesDispatcherModule
import com.hoc.comicapp.koin.dataModule
import com.hoc.comicapp.koin.networkModule
import com.hoc.comicapp.koin.viewModelModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.android.startKoin

@ExperimentalCoroutinesApi
class App : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin(
      this,
      listOf(
        networkModule,
        dataModule,
        coroutinesDispatcherModule,
        viewModelModule
      )
    )
  }
}