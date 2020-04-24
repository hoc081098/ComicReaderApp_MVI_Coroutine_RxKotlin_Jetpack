package com.hoc.comicapp.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.failure
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.SplashActivity
import com.hoc.comicapp.data.JsonAdaptersContainer
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class DownloadComicWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
  private val downloadComicsRepo by inject<DownloadComicsRepository>()
  private val jsonAdaptersContainer by inject<JsonAdaptersContainer>()
  private val dispatchers by inject<CoroutinesDispatchersProvider>()

  override suspend fun doWork(): Result {
    val (chapterLink, chapterJson, comicName, chapterComicName) = extractArgument()
      .fold(left = { return failure(workDataOf(it)) }, right = { it })

    val notificationManager = NotificationManagerCompat.from(applicationContext)
    val notificationBuilder = createNotificationBuilder(chapterComicName)

    return try {
      notificationManager.notify(
        chapterComicName,
        0,
        notificationBuilder.build()
      )

      downloadComicsRepo
        .downloadChapter(chapterLink)
        .collect {
          notificationManager.notify(
            chapterComicName,
            0,
            notificationBuilder
              .setProgress(100, it, false)
              .setContentText("$it %")
              .build()
          )

          setProgress(
            workDataOf(
              PROGRESS to it,
              COMIC_NAME to comicName,
              CHAPTER to chapterJson
            )
          )
        }

      notificationManager.notify(
        chapterComicName,
        0,
        notificationBuilder
          .setContentText("Download complete. Click to see all downloaded chapter")
          .setProgress(0, 0, false)
          .build()
      )

      success()
    } catch (e: Throwable) {
      Timber.d(e, "Exception: $e")

      notificationManager.notify(
        chapterComicName,
        0,
        notificationBuilder
          .setContentText("Download failed")
          .setProgress(0, 0, false)
          .build()
      )

      failure()
    }
  }

  private data class WorkerArgument(
    val chapterLink: String,
    val chapterJson: String,
    val comicName: String?,
    val chapterComicName: String,
  )

  private suspend fun extractArgument(): Either<Pair<String, String>, WorkerArgument> {
    // Extract arguments
    val chapterJson = inputData.getString(CHAPTER)
      ?: return (ERROR to "chapterJson is null").left()

    val (chapterLink, chapterName) = kotlin.runCatching {
      withContext(dispatchers.io) {
        // TODO: Remove @Suppress("BlockingMethodInNonBlockingContext"). This seem to be a IntelliJ Idea's bug.
        @Suppress("BlockingMethodInNonBlockingContext")
        jsonAdaptersContainer.comicDetailChapterAdapter.fromJson(chapterJson)
      }
    }.getOrNull() ?: return (ERROR to "chapter is null").left()

    val comicName = inputData.getString(COMIC_NAME)
    val chapterComicName = listOfNotNull(chapterName, comicName).joinToString(" - ")
    return WorkerArgument(
      chapterLink = chapterLink,
      chapterJson = chapterJson,
      comicName = comicName,
      chapterComicName = chapterComicName,
    ).right()
  }

  private fun createNotificationBuilder(chapterComicName: String): NotificationCompat.Builder {
    return NotificationCompat.Builder(applicationContext,
        applicationContext.getString(R.string.notification_channel_id))
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle("Download $chapterComicName")
      .setContentText("Downloading...")
      .setProgress(100, 0, false)
      .setAutoCancel(true)
      .setOngoing(false)
      .setPriority(PRIORITY_HIGH)
      .setOnlyAlertOnce(true)
      .setWhen(System.currentTimeMillis())
      .setContentIntent(
        PendingIntent.getActivity(
          applicationContext,
          0,
          Intent(applicationContext, SplashActivity::class.java),
          PendingIntent.FLAG_UPDATE_CURRENT
        )
      )
  }

  companion object {
    const val TAG = "DOWNLOAD_CHAPTER_TAG"
    const val ERROR = "ERROR"
    const val CHAPTER = "CHAPTER"
    const val COMIC_NAME = "COMIC_NAME"
    const val PROGRESS = "PROGRESS"
  }
}