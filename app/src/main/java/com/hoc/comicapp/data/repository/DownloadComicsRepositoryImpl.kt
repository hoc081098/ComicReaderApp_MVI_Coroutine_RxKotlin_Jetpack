package com.hoc.comicapp.data.repository

import android.app.Application
import androidx.lifecycle.map
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.identity
import arrow.core.left
import arrow.core.right
import com.hoc.comicapp.BuildConfig
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
import com.hoc.comicapp.domain.models.DownloadProgress
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.domain.models.LocalStorageError
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.utils.copyTo
import com.hoc.comicapp.utils.retryIO
import com.hoc.comicapp.worker.DownloadComicWorker
import com.hoc081098.flowext.utils.NULL_VALUE
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Date
import kotlin.time.DurationUnit
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
  private val jsonAdaptersContainer: JsonAdaptersContainer,
  private val analyticsService: AnalyticsService,
) : DownloadComicsRepository {

  override fun getDownloadedChapters(): Flow<List<DownloadedChapter>> =
    chapterDao
      .getAllChaptersFlow()
      .map { it.map(Mappers::entityToDomainModel) }

  override fun getDownloadedComics(): Observable<DomainResult<List<DownloadedComic>>> =
    chapterDao
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
      .onErrorReturn { errorMapper(it).left() }
      .subscribeOn(rxSchedulerProvider.io)

  override fun getDownloadedChapter(chapterLink: String): Flow<DomainResult<DownloadedChapter>> {
    val allChaptersF = chapterDao.getAllChaptersFlow().distinctUntilChanged()
    val chapterF = chapterDao.getByChapterLink(chapterLink).distinctUntilChanged()

    return combine(chapterF, allChaptersF) { chapter, chapters ->
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
      .catch { emit(errorMapper(it).left()) }
      .flowOn(dispatchersProvider.io)
  }

  override fun getDownloadedComic(link: String): Observable<DomainResult<DownloadedComic>> =
    comicDao
      .getByComicLink(link)
      .map<DomainResult<DownloadedComic>> { comicAndChapters ->
        Mappers.entityToDomainModel(
          comicAndChapters.apply {
            chapters = chapters.sortedByDescending { it.order }
          }
        ).right()
      }
      .onErrorReturn { errorMapper(it).left() }
      .subscribeOn(rxSchedulerProvider.io)

  override suspend fun deleteComic(comic: DownloadedComic) =
    either<Throwable, Unit> {
      val images = comic.chapters.flatMap { it.images }
      workManager.runCatching { cancelAllWorkByTag(comic.comicLink).await() }.bind()
      Either.catch { comicDao.delete(Mappers.domainToLocalEntity(comic)) }.bind()
      deleteImages(images).bind()
    }
      .tapLeft { Timber.e(it, "Error occurred while deleting downloaded comic=$comic") }
      .mapLeft(errorMapper)

  override suspend fun deleteDownloadedChapter(chapter: DownloadedChapter): DomainResult<Unit> =
    either<Throwable, Unit> {
      workManager.runCatching { cancelAllWorkByTag(chapter.chapterLink).await() }.bind()
      deleteChapterAndComicEntityIfNeeded(chapter).bind()
      deleteImages(chapter.images).bind()
    }
      .tapLeft { Timber.e(it, "Error occurred while deleting downloaded chapter=$chapter") }
      .mapLeft(errorMapper)

  override suspend fun enqueueDownload(
    chapter: DownloadedChapter,
    comicName: String,
    comicLink: String,
  ): DomainResult<Unit> = either<Throwable, Unit> {
    deleteDownloadedChapter(chapter).bind()

    workManager.runCatching {
      enqueue(
        buildDownloadWorkRequest(
          chapter,
          comicName,
          comicLink,
        )
      ).await()
    }.bind()
  }.tapLeft {
    Timber.e(
      it,
      "Error occurred when enqueuing download work, comicName=$comicName, chapter=$chapter"
    )
  }.mapLeft(errorMapper)

  @OptIn(ExperimentalTime::class)
  override fun downloadChapter(chapterLink: String): Flow<DownloadProgress> {
    return flow {
      Timber.d("$LOG_TAG Begin")
      val start = TimeSource.Monotonic.markNow()

      emit(DownloadProgress.require(0))

      val chapterDetail = retryIO(
        times = 3,
        initialDelay = 1_000,
        factor = 2.0
      ) { comicApiService.getChapterDetail(chapterLink) }
      val comicNameEscaped = chapterDetail.comicName.escapeFileName()
      val chapterNameEscaped = chapterDetail.chapterName.escapeFileName()
      val totalImageSize = chapterDetail.images.size
      Timber.d("$LOG_TAG Images.size = $totalImageSize")

      emit(DownloadProgress.require(10))

      val imagePaths = if (totalImageSize > 0) {
        downloadAndSaveImages(
          images = chapterDetail.images,
          comicName = comicNameEscaped,
          chapterName = chapterNameEscaped
        )
          .map { it to (10 + (it.size.toFloat() / totalImageSize) * 80).toInt() }
          .emitAllTo(this) { DownloadProgress.require(it.second) }
          ?.first ?: emptyList()
      } else {
        emptyList()
      }

      emit(DownloadProgress.require(80))

      val comicDetail = retryIO(
        times = 3,
        initialDelay = 1_000,
        factor = 2.0
      ) { comicApiService.getComicDetail(chapterDetail.comicLink) }
      val thumbnailPath = downloadComicThumbnail(
        thumbnailUrl = comicDetail.thumbnail,
        comicName = comicNameEscaped
      )

      emit(DownloadProgress.require(90))

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

      emit(DownloadProgress.require(100))

      val elapsed = start.elapsedNow()
      analyticsService.track(
        AnalyticsEvent.downloadChapter(
          chapterLink = chapterLink,
          chapterName = chapterDetail.chapterName,
          comicLink = chapterDetail.comicLink,
          comicName = chapterDetail.comicName,
          elapsedInMilliseconds = elapsed.toDouble(DurationUnit.MILLISECONDS),
          elapsedInString = elapsed.toString()
        )
      )

      Timber.d("$LOG_TAG Elapsed = $elapsed, Images = $imagePaths")
    }
      .flowOn(dispatchersProvider.io)
      .distinctUntilChanged()
  }

  // Private helper methods

  private fun buildDownloadWorkRequest(
    chapter: DownloadedChapter,
    comicName: String,
    comicLink: String,
  ): OneTimeWorkRequest {
    val chapterJson = jsonAdaptersContainer.comicDetailChapterAdapter.toJson(
      ComicDetail.Chapter(
        chapterLink = chapter.chapterLink,
        chapterName = chapter.chapterName,
        time = chapter.time,
        view = chapter.view
      )
    )

    return OneTimeWorkRequestBuilder<DownloadComicWorker>()
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
      .apply {
        if (BuildConfig.DEBUG) {
          addTag(DownloadComicWorker.TAG) // for debugging
        }
      }
      .addTag(chapter.chapterLink) // for cancelling work by chapter
      .addTag(comicLink) // for cancelling all works by comic
      .build()
  }

  /**
   * Delete chapter entity and delete comic entity if comic's chapters is empty
   */
  private suspend fun deleteChapterAndComicEntityIfNeeded(chapter: DownloadedChapter): Either<LocalStorageError.DatabaseError, Unit> =
    Either
      .catch {
        withContext(dispatchersProvider.io) {
          appDatabase.withTransaction {
            chapterDao.delete(Mappers.domainToLocalEntity(chapter))

            val chaptersCount =
              chapterDao.getCountByComicLink(chapter.comicLink).firstOrNull() ?: 0

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
          }
        }
      }
      .mapLeft { LocalStorageError.DatabaseError(it) }

  /**
   * Delete image files.
   */
  private suspend fun deleteImages(imagePaths: List<String>): Either<LocalStorageError.DeleteFileError, Unit> =
    if (imagePaths.isEmpty()) Unit.right()
    else {
      Either.catch {
        val success = withContext(dispatchersProvider.io) {
          imagePaths
            .mapNotNull { path ->
              File(application.filesDir, path)
                .takeIf { it.exists() }
                ?.delete()
            }
            .all(::identity)
        }
        if (!success) {
          throw LocalStorageError.DeleteFileError
        }
      }.mapLeft { LocalStorageError.DeleteFileError }
    }

  private suspend inline fun <R> WorkManager.runCatching(crossinline block: suspend WorkManager.() -> R): Either<Throwable, R> =
    Either.catch {
      withContext(dispatchersProvider.io) {
        block()
      }
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
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun downloadAndSaveImages(
    images: List<String>,
    comicName: String,
    chapterName: String,
  ): Flow<List<String>> {
    return images
      .withIndex()
      .asFlow()
      .map { (index, imageUrl) ->
        Timber.d("$LOG_TAG Begin $index $imageUrl")

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

            Timber.d("$LOG_TAG Done $index $imageUrl -> $imagePath")
            imagePath
          }
      }
      .scan(emptyList(), List<String>::plus)
  }

  private companion object {
    const val LOG_TAG = "[DOWNLOAD_COMIC_REPO]"
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

/**
 * Collects all the values from the given flow, transform them by [transformer] and emits them to the [collector].
 * It is a shorthand for `flow.collect { value -> emit(value) }`.
 *
 * @return The last element emitted by the flow, `null` if the flow was empty.
 */
private suspend fun <T, R> Flow<T>.emitAllTo(
  collector: FlowCollector<R>,
  transformer: (T) -> R,
): T? {
  var last: Any? = NULL_VALUE
  collect {
    last = it
    collector.emit(transformer(it))
  }
  return NULL_VALUE.unbox(last)
}
