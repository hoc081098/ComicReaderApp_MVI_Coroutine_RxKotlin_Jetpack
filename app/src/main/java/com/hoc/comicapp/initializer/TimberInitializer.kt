package com.hoc.comicapp.initializer

import android.content.Context
import androidx.startup.Initializer
import com.hoc.comicapp.BuildConfig
import com.hoc.comicapp.CrashlyticsLogger
import timber.log.Timber

@Suppress("unused")
class TimberInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    if (BuildConfig.DEBUG) {
      Timber.plant(
        Timber.DebugTree(),
        CrashlyticsLogger()
      )
    } else {
      Timber.plant(CrashlyticsLogger())
    }
    Timber.tag("Initializer").d("Timber initialized")
  }

  override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
