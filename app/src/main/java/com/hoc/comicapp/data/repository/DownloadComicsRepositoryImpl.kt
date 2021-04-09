package com.hoc.comicapp.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.JsonAdaptersContainer
import com.hoc.comicapp.data.Mappers
import com.hoc.comicapp.data.analytics.downloadChapter
import com.hoc.comicapp.data.local.AppDatabase
import com.hoc.comicapp.data.local.dao.ChapterDao
import com.hoc.comicapp.data.local.dao.ComicDao
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import com.hoc.comicapp.data.local.entities.ComicEntity
import com.hoc.comicapp.data.remote.ComicApiService
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.analytics.AnalyticsEvent
import com.hoc.comicapp.domain.analytics.AnalyticsService
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.domain.models.LocalStorageError
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.copyTo
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.retryIO
import com.hoc.comicapp.utils.right
import com.hoc.comicapp.worker.DownloadComicWorker
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Date
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

class DownloadComicsRepositoryImpl(
  private val comicApiService: ComicApiService,
  private val application: Application,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val comicDao: ComicDao,
  private val chapterDao: ChapterDao,
  private val appDatabase: AppDatabase,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val errorMapper: ErrorMapper,
  private val workManager: WorkManager,
  private val jsonAdapterConstraints: JsonAdaptersContainer,
  private val analyticsService: AnalyticsService,
) : DownloadComicsRepository {

  /*
   * Implement DownloadComicsRepository
   */

  override fun getDownloadedChapter(chapterLink: String): Flow<DomainResult<DownloadedChapter>> {
    val allChaptersF = chapterDao.getAllChaptersFlow().distinctUntilChanged()
    val chapterF = chapterDao.getByChapterLink(chapterLink).distinctUntilChanged()

    return chapterF
      .combine(allChaptersF) { chapter, chapters ->
        val index = chapters.indexOfFirst { it.chapterLink == chapterLink }
        Mappers.entityToDomainModel(chapter)
          .copy(
            chapters = chapters.map(Mappers::entityToDomainModel),
            prevChapterLink = chapters.getOrNull(index - 1)?.chapterLink,
            nextChapterLink = chapters.getOrNull(index + 1)?.chapterLink
          )
      }
      .map {
        @Suppress("USELESS_CAST")
        it.right() as DomainResult<DownloadedChapter>
      }
      .catch { emit(errorMapper.mapAsLeft(it)) }
      .flowOn(dispatchersProvider.io)
  }

  override fun getDownloadedComic(link: String): Observable<DomainResult<DownloadedComic>> {
    return comicDao
      .getByComicLink(link)
      .map<DomainResult<DownloadedComic>> { comicAndChapters ->
        Mappers.entityToDomainModel(
          comicAndChapters.apply {
            chapters = chapters.sortedByDescending { it.order }
          }
        ).right()
      }
      .onErrorReturn { errorMapper.mapAsLeft(it) }
      .subscribeOn(rxSchedulerProvider.io)
  }

  override suspend fun deleteComic(comic: DownloadedComic): DomainResult<Unit> {
    return runCatching {
      withContext(dispatchersProvider.main) {
        comicDao.delete(Mappers.domainToLocalEntity(comic))
      }
    }.fold(
      onSuccess = { Unit.right() },
      onFailure = { errorMapper.mapAsLeft(it) }
    )
  }

  override suspend fun enqueueDownload(
    chapter: DownloadedChapter,
    comicName: String,
  ): DomainResult<Unit> {
    return try {
      withContext(dispatchersProvider.io) {
        val chapterJson = jsonAdapterConstraints.comicDetailChapterAdapter.toJson(
          ComicDetail.Chapter(
            chapterLink = chapter.chapterLink,
            chapterName = chapter.chapterName,
            time = chapter.time,
            view = chapter.view
          )
        )

        val workRequest = OneTimeWorkRequestBuilder<DownloadComicWorker>()
          .setInputData(
            workDataOf(
              DownloadComicWorker.CHAPTER to chapterJson,
              DownloadComicWorker.COMIC_NAME to comicName
            )
          )
          .setConstraints(
            Constraints.Builder()
              .setRequiredNetworkType(NetworkType.CONNECTED)
              .setRequiresStorageNotLow(true)
              .build()
          )
          .addTag(DownloadComicWorker.TAG)
          .addTag(chapter.chapterLink)
          .build()

        deleteDownloadedChapter(chapter).fold(left = { throw it }, right = { Unit })
        workManager.enqueue(workRequest).await()

        Unit.right()
      }
    } catch (e: Throwable) {
      errorMapper.mapAsLeft(e)
    }
  }

  override suspend fun deleteDownloadedChapter(chapter: DownloadedChapter): DomainResult<Unit> {
    return try {
      workManager.cancelAllWorkByTag(chapter.chapterLink).await()
      deleteEntityAndImages(chapter)
    } catch (e: Exception) {
      errorMapper.mapAsLeft(e)
    }
  }

  override fun getDownloadedChapters(): LiveData<List<DownloadedChapter>> {
    return chapterDao.getAllChaptersLiveData().map { chapters ->
      chapters.map { Mappers.entityToDomainModel(it) }
    }
  }

  override fun getDownloadedComics(): Observable<DomainResult<List<DownloadedComic>>> {
    return chapterDao
      .getComicAndChapters()
      .map<DomainResult<List<DownloadedComic>>> { list ->
        list
          .map { item ->
            val entity = ComicAndChapters().also { copied ->
              copied.comic = item.comic
              copied.chapters = item.chapters
                .sortedByDescending { it.downloadedAt }
                .take(3)
            }
            Mappers.entityToDomainModel(entity)
          }
          .right()
      }
      .onErrorReturn { errorMapper.mapAsLeft(it) }
      .subscribeOn(rxSchedulerProvider.io)
  }

  @OptIn(ExperimentalTime::class)
  override fun downloadChapter(chapterLink: String): Flow<Int> {
    return flow {
      Timber.d("$tag Begin")
      val start = TimeSource.Monotonic.markNow()

      emit(0)

      val chapterDetail = retryIO(
        times = 3,
        initialDelay = 1_000,
        factor = 2.0
      ) { comicApiService.getChapterDetail(chapterLink) }
      val comicNameEscaped = chapterDetail.comicName.escapeFileName()
      val chapterNameEscaped = chapterDetail.chapterName.escapeFileName()
      val totalImageSize = chapterDetail.images.size
      Timber.d("$tag Images.size = $totalImageSize")

      emit(10)

      var imagePaths = emptyList<String>()
      downloadAndSaveImages(
        images = chapterDetail.images,
        comicName = comicNameEscaped,
        chapterName = chapterNameEscaped
      ).collect {
        val progress = (10 + (it.size.toFloat() / totalImageSize) * 80).toInt()
        emit(progress)
        imagePaths = it
      }

      val comicDetail = retryIO(
        times = 3,
        initialDelay = 1_000,
        factor = 2.0
      ) { comicApiService.getComicDetail(chapterDetail.comicLink) }
      val thumbnailPath = downloadComicThumbnail(
        thumbnailUrl = comicDetail.thumbnail,
        comicName = comicNameEscaped
      )

      appDatabase.withTransaction {
        comicDao.upsert(
          Mappers
            .responseToLocalEntity(comicDetail)
            .copy(thumbnail = thumbnailPath)
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
            order = comicDetail.chapters.size - currentIndex,
            downloadedAt = Date()
          )
        )
      }

      emit(100)

      val elapsed = start.elapsedNow()
      analyticsService.track(
        AnalyticsEvent.downloadChapter(
          chapterLink = chapterLink,
          chapterName = chapterDetail.chapterName,
          comicLink = chapterDetail.comicLink,
          comicName = chapterDetail.comicName,
          elapsedInMilliseconds = elapsed.inMilliseconds,
          elapsedInString = elapsed.toString()
        )
      )

      Timber.d("$tag Elapsed = $elapsed, Images = $imagePaths")
    }.flowOn(dispatchersProvider.io)
  }

  /*
   * Private helper methods
   */

  /**
   * Delete chapter entity and delete comic entity if comic's chapters is empty
   */
  private suspend fun deleteEntityAndImages(chapter: DownloadedChapter): DomainResult<Unit> {
    return runCatching {
      withContext(dispatchersProvider.io) {
        chapterDao.delete(Mappers.domainToLocalEntity(chapter))

        val chaptersCount = chapterDao.getCountByComicLink(chapter.comicLink).firstOrNull() ?: 0
        if (chaptersCount == 0) {
          comicDao.delete(
            ComicEntity(
              comicLink = chapter.comicLink,
              view = "",
              categories = emptyList(),
              authors = emptyList(),
              thumbnail = "",
              title = "",
              lastUpdated = "",
              shortenedContent = "",
              remoteThumbnail = ""
            )
          )
        }

        chapter
          .images
          .map { File(application.filesDir, it) }
          .all(File::delete)
      }
    }.fold(
      {
        if (it) {
          Unit.right()
        } else {
          LocalStorageError.DeleteFileError.left()
        }
      },
      { errorMapper.mapAsLeft(it) }
    )
  }

  /**
   * Download comic thumbnail
   * @param thumbnailUrl image url
   * @param comicName comic name
   * @return image path
   */
  private suspend fun downloadComicThumbnail(thumbnailUrl: String, comicName: String): String {
    return retryIO(
      times = 3,
      initialDelay = 1_000,
      factor = 2.0
    ) { comicApiService.downloadFile(thumbnailUrl) }
      .use { responseBody ->
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

  /**
   * @return a [Flow] emit downloaded image paths
   */
  private fun downloadAndSaveImages(
    images: List<String>,
    comicName: String,
    chapterName: String,
  ): Flow<List<String>> {
    return flow {
      val imagePaths = mutableListOf<String>()

      for ((index, imageUrl) in images.withIndex()) {
        Timber.d("$tag Begin $index $imageUrl")

        retryIO(
          times = 3,
          initialDelay = 1_000,
          factor = 2.0
        ) { comicApiService.downloadFile(imageUrl) }
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

            emit(imagePaths)
            Timber.d("$tag Done $index $imageUrl -> $imagePath")
          }
      }

      emit(imagePaths)
    }
  }

  /**
   * Escape file name: only allow letters, numbers, dots and dashes
   */
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
