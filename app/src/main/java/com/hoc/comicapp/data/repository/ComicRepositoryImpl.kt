package com.hoc.comicapp.data.repository

import arrow.core.Either
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.Mappers
import com.hoc.comicapp.data.analytics.readChapter
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.data.local.dao.ComicDao
import com.hoc.comicapp.data.local.entities.ComicEntity
import com.hoc.comicapp.data.remote.ComicApiService
import com.hoc.comicapp.data.remote.response.ComicDetailResponse
import com.hoc.comicapp.data.remote.response.ComicResponse
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.analytics.AnalyticsEvent
import com.hoc.comicapp.domain.analytics.AnalyticsService
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.CategoryDetailPopularComic
import com.hoc.comicapp.domain.models.ChapterDetail
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.utils.Cache
import com.hoc.comicapp.utils.parZipEither
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(
  ExperimentalTime::class,
  ObsoleteCoroutinesApi::class
)
class ComicRepositoryImpl(
  private val errorMapper: ErrorMapper,
  private val comicApiService: ComicApiService,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val favoriteComicsDataSource: FavoriteComicsDataSource,
  private val comicDao: ComicDao,
  private val analyticsService: AnalyticsService,
  appCoroutineScope: CoroutineScope,
) : ComicRepository {
  private val cache = Cache<RequestCacheKey, Any>(
    maxSize = 8,
    entryLifetime = 60.seconds
  )
  private val actor = appCoroutineScope.actor<ComicEntity>(capacity = Channel.BUFFERED) {
    for (entity in this) {
      _updateDownloadedComic(entity)
    }
  }

  @Suppress("FunctionName")
  private suspend fun _updateDownloadedComic(entity: ComicEntity) {
    measureTime {
      Timber.d("[UPDATE_COMICS] [1] Start ${entity.title}")

      val oldEntity = comicDao.findByComicLink(entity.comicLink) ?: return@measureTime
      val newEntity = entity.copy(thumbnail = oldEntity.thumbnail)

      if (oldEntity != newEntity) {
        comicDao.update(newEntity)
        Timber.d("[UPDATE_COMICS] [2] Done update ${entity.title}")
      }
    }.let { Timber.d("[UPDATE_COMICS] [3] Time = $it ${entity.title}") }
  }

  private suspend inline fun <reified T : Any> executeApiRequest(
    path: String,
    queryItems: Map<String, Any?> = emptyMap(),
    crossinline request: suspend ComicApiService.() -> T,
  ): DomainResult<T> {
    val cacheKey = buildKey(path, queryItems)

    return Either.catch(errorMapper) {
      when (val cachedResponse = cache[cacheKey] as? T) {
        null -> {
          Timber.d("ComicRepositoryImpl::$cacheKey [MISS] request...")

          withContext(dispatchersProvider.io) {
            comicApiService
              .request()
              .also { cache[cacheKey] = it }
          }
        }
        else -> {
          Timber.d("ComicRepositoryImpl::$cacheKey [HIT] cached")

          delay(250)
          cachedResponse
        }
      }
    }.tapLeft { throwable ->
      Timber.e(throwable, "ComicRepositoryImpl::$cacheKey [ERROR] $throwable")
      delay(500)
    }
  }

  /*
   * Implement ComicRepository
   */

  override suspend fun getCategoryDetailPopular(categoryLink: String): DomainResult<List<CategoryDetailPopularComic>> {
    return executeApiRequest(
      "getCategoryDetailPopular",
      mapOf("categoryLink" to categoryLink)
    ) {
      getCategoryDetailPopular(categoryLink)
        .map(Mappers::responseToDomainModel)
    }
  }

  override suspend fun getCategoryDetail(
    categoryLink: String,
    page: Int,
  ): DomainResult<List<Comic>> {
    return executeApiRequest(
      "getCategoryDetail",
      mapOf(
        "categoryLink" to categoryLink,
        "page" to page
      )
    ) {
      getCategoryDetail(categoryLink, page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mappers::responseToDomainModel)
    }
  }

  override suspend fun getMostViewedComics(page: Int): DomainResult<List<Comic>> {
    return executeApiRequest(
      "getMostViewedComics",
      mapOf("page" to page)
    ) {
      comicApiService
        .getMostViewedComics(page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mappers::responseToDomainModel)
    }
  }

  override suspend fun getUpdatedComics(page: Int): DomainResult<List<Comic>> {
    return executeApiRequest(
      "getUpdatedComics",
      mapOf("page" to page)
    ) {
      comicApiService
        .getUpdatedComics(page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mappers::responseToDomainModel)
    }
  }

  override suspend fun getNewestComics(page: Int): DomainResult<List<Comic>> {
    return executeApiRequest(
      "getNewestComics",
      mapOf("page" to page)
    ) {
      comicApiService
        .getNewestComics(page)
        .also { Timber.d("ComicRepositoryImpl::getNewestComics [RESPONSE] $it") }
        .also(::updateFavoritesAndDownloaded)
        .map(Mappers::responseToDomainModel)
    }
  }

  override suspend fun refreshAll(): DomainResult<Triple<List<Comic>, List<Comic>, List<Comic>>> {
    return parZipEither(
      ctx = dispatchersProvider.io,
      fa = { getNewestComics() },
      fb = { getMostViewedComics() },
      fc = { getUpdatedComics() },
    ) { a, b, c -> Triple(a, b, c) }
      .mapLeft { error ->
        Timber.e(error, "ComicRepositoryImpl::refreshAll [ERROR] $error")
        delay(500)

        error
      }
  }

  override suspend fun getComicDetail(comicLink: String): DomainResult<ComicDetail> {
    return executeApiRequest(
      "getComicDetail",
      mapOf("comicLink" to comicLink)
    ) {
      comicApiService
        .getComicDetail(comicLink)
        .also(::updateFavoritesAndDownloaded)
        .let(Mappers::responseToDomainModel)
    }
  }

  override suspend fun getChapterDetail(chapterLink: String): DomainResult<ChapterDetail> {
    return executeApiRequest(
      "getChapterDetail",
      mapOf("chapterLink" to chapterLink)
    ) {
      comicApiService
        .getChapterDetail(chapterLink)
        .let(Mappers::responseToDomainModel)
    }.also { result ->
      result.orNull()?.let { detail ->
        analyticsService.track(
          AnalyticsEvent.readChapter(
            chapterLink = detail.chapterLink,
            chapterName = detail.chapterName,
            imagesSize = detail.images.size
          )
        )
      }
    }
  }

  override suspend fun getAllCategories(): DomainResult<List<Category>> {
    return executeApiRequest("getAllCategories") {
      comicApiService
        .getAllCategories()
        .map(Mappers::responseToDomainModel)
    }
  }

  override suspend fun searchComic(query: String, page: Int): DomainResult<List<Comic>> {
    return executeApiRequest(
      "searchComic",
      mapOf(
        "query" to query,
        "page" to page
      )
    ) {
      comicApiService
        .searchComic(query, page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mappers::responseToDomainModel)
    }
  }

  /*
   * Private helper methods
   */

  private fun updateFavoritesAndDownloaded(comics: List<ComicResponse>) {
    comics
      .map { Mappers.responseToFirebaseEntity(it) }
      .let { favoriteComicsDataSource.update(it) }
  }

  private fun updateFavoritesAndDownloaded(comicDetail: ComicDetailResponse) {
    // update favorite
    val entity = Mappers.responseToFirebaseEntity(comicDetail)
    favoriteComicsDataSource.update(listOf(entity))

    // update downloaded
    actor.trySend(Mappers.responseToLocalEntity(comicDetail))
  }

  private companion object {
    data class RequestCacheKey(
      val path: String,
      val queryItems: Map<String, String>,
    )

    private fun buildKey(path: String, queryItems: Map<String, Any?>): RequestCacheKey {
      return RequestCacheKey(
        path = path,
        queryItems = queryItems.entries
          .mapNotNull { (k, v) ->
            if (v == null) null
            else k to v.toString()
          }
          .toMap()
      )
    }
  }
}
