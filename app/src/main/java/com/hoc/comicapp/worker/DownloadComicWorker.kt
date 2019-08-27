package com.hoc.comicapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.utils.fold
import kotlinx.coroutines.coroutineScope
import org.koin.core.KoinComponent
import org.koin.core.inject

class DownloadComicWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {
  private val downloadComicsRepo by inject<DownloadComicsRepository>()

  override suspend fun doWork(): Result {
    return coroutineScope {
      when (val chapterLink = inputData.getString(CHAPTER_LINK)) {
        null -> Result.failure(workDataOf(ERROR to "chapterUrl is null"))
        else -> {
          downloadComicsRepo
            .downloadChapter(chapterLink)
            .fold(
              left = { Result.failure(workDataOf(ERROR to "Error: ${it.getMessage()}")) },
              right = { Result.success() }
            )
        }
      }
    }
  }

  companion object {
    const val ERROR = "ERROR"
    const val CHAPTER_LINK = "CHAPTER_LINK"
  }
}
