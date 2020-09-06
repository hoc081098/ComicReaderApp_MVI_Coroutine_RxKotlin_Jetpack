package com.hoc.comicapp.initializer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.startup.Initializer
import com.hoc.comicapp.R
import timber.log.Timber

@Suppress("unused")
class NotificationInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    context.run {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          getString(R.string.notification_channel_id),
          getString(R.string.notification_channel_name),
          NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = getString(R.string.notification_channel_description) }

        getSystemService(NotificationManager::class.java)!!.createNotificationChannel(channel)
      }
    }
    Timber.tag("Initializer").d("Notification initialized")
  }

  override fun dependencies(): List<Class<out Initializer<*>>> =
    listOf(TimberInitializer::class.java)
}
