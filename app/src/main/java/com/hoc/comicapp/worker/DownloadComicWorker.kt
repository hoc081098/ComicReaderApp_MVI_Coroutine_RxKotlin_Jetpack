package com.hoc.comicapp.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.SplashActivity
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.koin.core.KoinComponent
import org.koin.core.inject

@ExperimentalCoroutinesApi
class DownloadComicWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {
  private val downloadComicsRepo by inject<DownloadComicsRepository>()

  override suspend fun doWork(): Result {
    val chapterLink =
      inputData.getString(CHAPTER_LINK) ?: return Result.failure(workDataOf(ERROR to "chapterUrl is null"))

    val notificationBuilder =
      NotificationCompat.Builder(applicationContext, applicationContext.getString(R.string.notification_channel_id))
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle("Download chapter")
        .setContentText("Downloading...")
        .setProgress(100, 0, false)
        .setAutoCancel(true)
        .setOngoing(true)
        .setWhen(System.currentTimeMillis())
        .setContentIntent(
          PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, SplashActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
          )
        )
    val notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
    notificationManagerCompat.notify(1, notificationBuilder.build())

    downloadComicsRepo
      .downloadChapter(chapterLink)
      .collect {
        notificationManagerCompat.notify(
          1,
          notificationBuilder
            .setProgress(100, it, false)
            .setContentText("$it %")
            .build()
        )

        setProgress(workDataOf(PROGRESS to it))
      }

    notificationManagerCompat.notify(
      1,
      notificationBuilder
        .setContentText("Download complete")
        .setProgress(0, 0, false)
        .build()
    )

    return Result.success()
  }

  companion object {
    const val ERROR = "ERROR"
    const val CHAPTER_LINK = "CHAPTER_LINK"
    const val PROGRESS = "PROGRESS"
  }
}