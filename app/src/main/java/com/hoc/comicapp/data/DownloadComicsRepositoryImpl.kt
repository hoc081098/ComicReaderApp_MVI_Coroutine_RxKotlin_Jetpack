package com.hoc.comicapp.data

import android.app.Application
import androidx.room.withTransaction
import com.hoc.comicapp.data.local.AppDatabase
import com.hoc.comicapp.data.local.dao.ChapterDao
import com.hoc.comicapp.data.local.dao.ComicDao
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicEntity
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
  private val retrofit: Retrofit,
  private val comicDao: ComicDao,
  private val chapterDao: ChapterDao,
  private val appDatabase: AppDatabase
) : DownloadComicsRepository {
  override suspend fun downloadChapter(chapterLink: String): Either<ComicAppError, Unit> {
    return try {
      withContext(dispatcherProvider.io) {
        Timber.d("$tag Begin")

        val chapterDetail = comicApiService.getChapterDetail(chapterLink)

        val comicNameEscaped = chapterDetail.comicName.escapeFileName()
        val chapterNameEscaped = chapterDetail.chapterName.escapeFileName()
        Timber.d("$tag Images.size = ${chapterDetail.images.size}")

        val imagePaths = downloadAndSaveImages(
          images = chapterDetail.images,
          comicName = comicNameEscaped,
          chapterName = chapterNameEscaped
        )

        val comicDetail = comicApiService.getComicDetail(chapterDetail.comicLink)
        val thumbnailPath = downloadComicThumbnail(
          thumbnailUrl = comicDetail.thumbnail,
          comicName = comicNameEscaped
        )

        appDatabase.withTransaction {
          comicDao.upsert(
            ComicEntity(
              authors = comicDetail.authors.map {
                ComicEntity.Author(
                  link = it.link,
                  name = it.name
                )
              },
              categories = comicDetail.categories.map {
                ComicEntity.Category(
                  link = it.link,
                  name = it.name
                )
              },
              lastUpdated = comicDetail.lastUpdated,
              comicLink = comicDetail.link,
              shortenedContent = comicDetail.shortenedContent,
              thumbnail = thumbnailPath,
              title = comicDetail.title,
              view = comicDetail.view
            )
          )

          val currentIndex = comicDetail.chapters.indexOfFirst { it.chapterLink == chapterLink }
          val currentChapter = comicDetail.chapters[currentIndex]

          chapterDao.upsert(
            ChapterEntity(
              chapterLink = chapterLink,
              view = currentChapter.view,
              comicLink = comicDetail.link,
              images = imagePaths,
              time = currentChapter.time,
              chapterName = chapterDetail.chapterName,
              order = comicDetail.chapters.size - currentIndex
            )
          )
        }

        Timber.d("$tag Images = $imagePaths")
        Unit.right()
      }
    } catch (throwable: Throwable) {
      throwable.toError(retrofit).left()
    }
  }

  private suspend fun downloadComicThumbnail(thumbnailUrl: String, comicName: String): String {
    return comicApiService.downloadFile(thumbnailUrl).use { responseBody ->
      val imagePath = listOf(
        "images",
        comicName,
        "thumbnail.png"
      ).joinToString(File.separator)

      responseBody.byteStream().copyTo(
        File(
          application.filesDir.path,
          imagePath
        ),
        overwrite = true
      )

      imagePath
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

  private fun String.escapeFileName(): String {
    return replace(
      "[^a-zA-Z0-9.\\-]".toRegex(),
      replacement = "_"
    )
  }

  private companion object {
    const val tag = "[DOWNLOAD_COMIC_REPO]"
  }
}