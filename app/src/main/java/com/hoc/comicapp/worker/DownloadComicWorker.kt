package com.hoc.comicapp.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result.failure
import androidx.work.ListenableWorker.Result.success
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.SplashActivity
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.JsonAdaptersContainer
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.initializer.startKoinIfNeeded
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadComicWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {
  private val downloadComicsRepo by inject<DownloadComicsRepository>()
  private val jsonAdaptersContainer by inject<JsonAdaptersContainer>()
  private val dispatchers by inject<CoroutinesDispatchersProvider>()
  private val errorMapper by inject<ErrorMapper>()

  init {
    appContext.startKoinIfNeeded()
  }

  @SuppressLint("RestrictedApi")
  override suspend fun doWork(): Result {
    val (chapterLink, chapterJson, comicName, chapterComicName) = extractArgument()
      .getOrHandle { return failure(workDataOf(it)) }

    val notificationManager = NotificationManagerCompat.from(applicationContext)
    val notificationBuilder = createNotificationBuilder(
      chapterComicName = chapterComicName,
      chapterLink = chapterLink
    )

    downloadComicsRepo
      .downloadChapter(chapterLink)
      .map { it.value }
      .onStart {
        notificationManager.notify(
          chapterLink,
          0,
          notificationBuilder.build()
        )
      }
      .onEach {
        notificationManager.notify(
          chapterLink,
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
      .onCompletion { throwable ->
        when (throwable) {
          null -> {
            notificationManager.notify(
              chapterLink,
              0,
              notificationBuilder
                .setContentText("Download complete. Click to see all downloaded chapters")
                .apply { mActions.clear() }
                .setProgress(0, 0, false)
                .build()
            )

            Timber.d("Download success.")
          }
          else -> {
            notificationManager.notify(
              chapterLink,
              0,
              notificationBuilder
                .setContentText(
                  if (throwable is CancellationException) "Download cancelled"
                  else "Download failed: ${errorMapper(throwable).getMessage()}"
                )
                .apply { mActions.clear() }
                .setProgress(0, 0, false)
                .build()
            )

            Timber.e(throwable, "Download failed. Throwable: $throwable")
          }
        }
      }
      .collect()

    return success()
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

    val (chapterLink, chapterName) = kotlin
      .runCatching {
        withContext(dispatchers.io) {
          jsonAdaptersContainer.comicDetailChapterAdapter.fromJson(chapterJson)
        }
      }
      .getOrNull() ?: return (ERROR to "chapter is null").left()

    val comicName = inputData.getString(COMIC_NAME)
    val chapterComicName = listOfNotNull(chapterName, comicName).joinToString(" - ")
    return WorkerArgument(
      chapterLink = chapterLink,
      chapterJson = chapterJson,
      comicName = comicName,
      chapterComicName = chapterComicName,
    ).right()
  }

  private fun createNotificationBuilder(
    chapterComicName: String,
    chapterLink: String,
  ): NotificationCompat.Builder {
    val cancelIntent = PendingIntent.getBroadcast(
      applicationContext,
      SystemClock.uptimeMillis().toInt(),
      ComicAppBroadcastReceiver.makeIntent(
        applicationContext,
        ComicAppBroadcastReceiver.Action.CancelDownload(
          workerId = ParcelUuid(id),
          chapterLink = chapterLink,
          chapterComicName = chapterComicName,
        )
      ),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat
      .Builder(
        applicationContext,
        applicationContext.getString(R.string.notification_channel_id)
      )
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
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      )
      .addAction(R.drawable.ic_close_white_24dp, "Cancel", cancelIntent)
  }

  companion object {
    const val TAG = "DOWNLOAD_CHAPTER_TAG"
    const val ERROR = "ERROR"
    const val CHAPTER = "CHAPTER"
    const val COMIC_NAME = "COMIC_NAME"
    const val PROGRESS = "PROGRESS"
  }
}
