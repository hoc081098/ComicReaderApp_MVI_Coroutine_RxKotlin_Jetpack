package com.hoc.comicapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.hoc.comicapp.koin.appModule
import com.hoc.comicapp.koin.dataModule
import com.hoc.comicapp.koin.networkModule
import com.hoc.comicapp.koin.viewModelModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

@ExperimentalCoroutinesApi
class App : Application() {
  override fun onCreate() {
    super.onCreate()

    startKoin {
      // use AndroidLogger as Koin Logger - default Level.INFO
      androidLogger()

      // use the Android context given there
      androidContext(this@App)

      modules(
        networkModule,
        dataModule,
        appModule,
        viewModelModule
      )
    }
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    }

    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        getString(R.string.notification_channel_id),
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply { description = getString(R.string.notification_channel_description) }

      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }
}