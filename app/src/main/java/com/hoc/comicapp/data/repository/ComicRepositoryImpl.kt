package com.hoc.comicapp.data.repository

import com.hoc.comicapp.data.Mapper
import com.hoc.comicapp.data.remote.ComicApiService
import com.hoc.comicapp.domain.models.*
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import timber.log.Timber

class ComicRepositoryImpl(
  private val retrofit: Retrofit,
  private val comicApiService: ComicApiService,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : ComicRepository {
  private suspend fun <T> executeApiRequest(
    tag: String,
    request: suspend ComicApiService.() -> T
  ): Either<ComicAppError, T> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .request()
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::$tag $throwable", throwable)
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
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getMostViewedComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("getMostViewedComics") {
      comicApiService
        .getMostViewedComics(page)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getUpdatedComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("getUpdatedComics") {
      comicApiService
        .getUpdatedComics(page)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getNewestComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return executeApiRequest("getNewestComics") {
      comicApiService
        .getNewestComics(page)
        .map(Mapper::responseToDomainModel)
    }
  }

  override suspend fun getComicDetail(comicLink: String): Either<ComicAppError, ComicDetail> {
    return executeApiRequest("getComicDetail") {
      comicApiService
        .getComicDetail(comicLink)
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
        .map(Mapper::responseToDomainModel)
    }
  }
}