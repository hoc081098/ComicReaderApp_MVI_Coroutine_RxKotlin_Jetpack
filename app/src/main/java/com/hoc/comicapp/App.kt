package com.hoc.comicapp

import android.app.Application
import com.hoc.comicapp.koin.networkModule
import org.koin.android.ext.android.startKoin

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin(
      this,
      listOf(networkModule)
    )
  }
}