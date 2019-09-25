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
import com.hoc.comicapp.domain.models.ComicDetail.Chapter
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.koin.appModule
import com.hoc.comicapp.koin.dataModule
import com.hoc.comicapp.koin.networkModule
import com.squareup.moshi.Moshi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import timber.log.Timber

@ExperimentalCoroutinesApi
class DownloadComicWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {
  private val koin = koinApplication {
    androidContext(applicationContext)
    modules(dataModule, networkModule, appModule)
  }.koin

  private val downloadComicsRepo by koin.inject<DownloadComicsRepository>()
  private val moshi by koin.inject<Moshi>()

  override suspend fun doWork(): Result {
    val chapterJson = inputData.getString(CHAPTER)
      ?: return Result.failure(workDataOf(ERROR to "chapterJson is null"))

    val (chapterLink, chapterName) = moshi.adapter<Chapter>(Chapter::class.java).fromJson(chapterJson)
      ?: return Result.failure(workDataOf(ERROR to "chapter is null"))

    val comicName = inputData.getString(COMIC_NAME)
    val chapterComicName = listOfNotNull(chapterName, comicName).joinToString(" - ")

    val notificationBuilder =
      NotificationCompat.Builder(applicationContext, applicationContext.getString(R.string.notification_channel_id))
        .setSmallIcon(R.mipmap.ic_launcher_round)
        .setContentTitle("Downloading $chapterComicName")
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

    return try {
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
          .setContentText("Download complete. Click to see all downloaded chapter")
          .setProgress(0, 0, false)
          .build()
      )

      Result.success()
    } catch (e: Exception) {
      Timber.d("Exception: $e", e)
      notificationManagerCompat.cancel(1)
      Result.failure()
    }
  }

  companion object {
    const val TAG = "DOWNLOAD_CHAPTER_TAG"
    const val ERROR = "ERROR"
    const val CHAPTER = "CHAPTER"
    const val COMIC_NAME = "COMIC_NAME"
    const val PROGRESS = "PROGRESS"
  }
}