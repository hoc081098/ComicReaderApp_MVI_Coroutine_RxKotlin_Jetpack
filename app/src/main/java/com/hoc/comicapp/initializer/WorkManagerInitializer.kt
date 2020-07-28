package com.hoc.comicapp.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import timber.log.Timber

@Suppress("unused")
class WorkManagerInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    WorkManager.initialize(
      context,
      Configuration.Builder()
        .setMinimumLoggingLevel(Log.INFO)
        .build()
    )
    Timber.tag("Initializer").d("WorkManager initialized")
  }

  override fun dependencies(): List<Class<out Initializer<*>>> =
    listOf(TimberInitializer::class.java)
}