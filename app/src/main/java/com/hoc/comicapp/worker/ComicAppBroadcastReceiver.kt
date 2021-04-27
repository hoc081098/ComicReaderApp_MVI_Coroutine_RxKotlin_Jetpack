package com.hoc.comicapp.worker

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import androidx.work.await
import com.hoc.comicapp.R
import com.hoc.comicapp.initializer.startKoinIfNeeded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class ComicAppBroadcastReceiver : BroadcastReceiver(), KoinComponent {
  private val workManager by inject<WorkManager>()
  private val appCoroutineScope by inject<CoroutineScope>()

  override fun onReceive(context: Context?, intent: Intent?) {
    context ?: return
    intent ?: return

    context.startKoinIfNeeded()

    when (
      val action = intent.getParcelableExtra<Action>(ACTION)
        .also { Timber.d("Action = $it") }
        ?: return
    ) {
      is Action.CancelDownload -> cancelDownload(action, context)
    }
  }

  private fun cancelDownload(action: Action.CancelDownload, context: Context) {
    val pendingResult = goAsync()

    appCoroutineScope.launch {
      kotlin
        .runCatching {
          workManager
            .cancelWorkById(action.workerId.uuid)
            .result
            .await()
        }
        .onSuccess {
          NotificationManagerCompat
            .from(context)
            .notify(
              action.chapterLink,
              0,
              createCancelledNotification(
                context,
                action.chapterComicName
              )
            )
        }
      pendingResult.finish()
    }
  }

  private fun createCancelledNotification(
    context: Context,
    chapterComicName: String,
  ): Notification {
    return NotificationCompat
      .Builder(
        context,
        context.getString(R.string.notification_channel_id)
      )
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle("Download $chapterComicName")
      .setContentText("Download cancelled")
      .setAutoCancel(true)
      .setOngoing(false)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setWhen(System.currentTimeMillis())
      .build()
  }

  companion object {
    private const val ACTION = "ComicAppBroadcastReceiver.Action"

    fun makeIntent(context: Context, action: Action): Intent {
      return Intent(
        context,
        ComicAppBroadcastReceiver::class.java
      ).apply { putExtra(ACTION, action) }
    }
  }

  sealed class Action : Parcelable {
    @Parcelize
    data class CancelDownload(
      val workerId: ParcelUuid,
      val chapterLink: String,
      val chapterComicName: String,
    ) : Action()
  }
}
