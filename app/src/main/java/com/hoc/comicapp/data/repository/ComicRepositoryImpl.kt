package com.hoc.comicapp.data.repository

import com.hoc.comicapp.data.Mapper
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.data.local.dao.ComicDao
import com.hoc.comicapp.data.local.entities.ComicEntity
import com.hoc.comicapp.data.remote.ComicApiService
import com.hoc.comicapp.data.remote.response.ComicDetailResponse
import com.hoc.comicapp.data.remote.response.ComicResponse
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.CategoryDetailPopularComic
import com.hoc.comicapp.domain.models.ChapterDetail
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import timber.log.Timber
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@ExperimentalTime
@ObsoleteCoroutinesApi
class ComicRepositoryImpl(
  private val retrofit: Retrofit,
  private val comicApiService: ComicApiService,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val favoriteComicsDataSource: FavoriteComicsDataSource,
  private val comicDao: ComicDao,
  appCoroutineScope: CoroutineScope
) : ComicRepository {
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

  private suspend fun <T> executeApiRequest(
    tag: String,
    request: suspend ComicApiService.() -> T
  ): Either<ComicAppError, T> {
    return try {
      withContext(dispatchersProvider.io) {
        comicApiService
          .request()
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::$tag $throwable", throwable)
      delay(500)
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getCategoryDetailPopular(categoryLink: String): Either<ComicAppError, List<CategoryDetailPopularComic>> {
    return executeApiRequest("getCategoryDetailPopular") {
      getCategoryDetailPopular(categoryLink)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getCategoryDetail(categoryLink: String, page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("getCategoryDetail") {
      getCategoryDetail(categoryLink, page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getMostViewedComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("getMostViewedComics") {
      comicApiService
        .getMostViewedComics(page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getUpdatedComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("getUpdatedComics") {
      comicApiService
        .getUpdatedComics(page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getNewestComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("getNewestComics") {
      comicApiService
        .getNewestComics(page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getComicDetail(comicLink: String): Either<ComicAppError, ComicDetail> {
    return executeApiRequest("getComicDetail") {
      comicApiService
        .getComicDetail(comicLink)
        .also(::updateFavoritesAndDownloaded)
        .let(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getChapterDetail(chapterLink: String): Either<ComicAppError, ChapterDetail> {
    return executeApiRequest("chapterLink") {
      comicApiService
        .getChapterDetail(chapterLink)
        .let(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getAllCategories(): Either<ComicAppError, List<Category>> {
    return executeApiRequest("getAllCategories") {
      comicApiService
        .getAllCategories()
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun searchComic(query: String, page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("searchComic") {
      comicApiService
        .searchComic(query, page)
        .also(::updateFavoritesAndDownloaded)
        .map(Mapper::responseToDomainModel)
    }
  }

  private fun updateFavoritesAndDownloaded(comics: List<ComicResponse>) {
    comics
      .map { Mapper.responseToFirebaseEntity(it) }
      .let { favoriteComicsDataSource.update(it) }
  }

  private fun updateFavoritesAndDownloaded(comicDetail: ComicDetailResponse) {
    // update favorite
    val entity = Mapper.responseToFirebaseEntity(comicDetail)
    favoriteComicsDataSource.update(listOf(entity))

    // update downloaded
    actor.offer(Mapper.responseToLocalEntity(comicDetail))
  }
}