package com.hoc.comicapp.data

import android.app.Application
import com.hoc.comicapp.data.remote.ComicApiService
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.copyTo
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File

class DownloadComicsRepositoryImpl(
  private val comicApiService: ComicApiService,
  private val application: Application,
  private val dispatcherProvider: CoroutinesDispatcherProvider,
  private val retrofit: Retrofit
) : DownloadComicsRepository {
  override suspend fun downloadChapter(chapterLink: String): Either<ComicAppError, Unit> {
    return try {
      withContext(dispatcherProvider.io) {
        Timber.d("$tag Begin")

        val chapterDetail = comicApiService.getChapterDetail(chapterLink)
        val comicName = "Testing"
        val chapterName = chapterDetail.chapterName.replace(
          "[^a-zA-Z0-9.\\-]".toRegex(),
          replacement = "_"
        )
        Timber.d("$tag Images.size = ${chapterDetail.images.size}")


        val imagePaths = downloadAndSaveImages(
          chapterDetail.images,
          comicName,
          chapterName
        )

        Timber.d("$tag Images = $imagePaths")
        Unit.right()
      }
    } catch (throwable: Throwable) {
      throwable.toError(retrofit).left()
    }
  }

  private suspend fun downloadAndSaveImages(
    images: List<String>,
    comicName: String,
    chapterName: String
  ): List<String> {
    val imagePaths = mutableListOf<String>()

    for ((index, imageUrl) in images.withIndex()) {
      Timber.d("$tag Begin $index $imageUrl")

      comicApiService
        .downloadFile(imageUrl)
        .use { responseBody ->
          val imagePath = listOf(
            "images",
            comicName,
            chapterName,
            "images_$index.png"
          ).joinToString(File.separator)

          responseBody.byteStream().copyTo(
            File(
              application.filesDir.path,
              imagePath
            ),
            overwrite = true
          )

          imagePaths += imagePath
          Timber.d("$tag Done $index $imageUrl -> $imagePath")
        }
    }

    return imagePaths
  }

  private companion object {
    const val tag = "[DOWNLOAD_COMIC_REPO]"
  }
}