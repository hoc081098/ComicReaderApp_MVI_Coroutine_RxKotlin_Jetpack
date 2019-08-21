package com.hoc.comicapp.data

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
  override suspend fun getMostViewedComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .getMostViewedComics(page)
          .map(Mapper::responseToDomainModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::getMostViewedComics $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getUpdatedComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .getUpdatedComics(page)
          .map(Mapper::responseToDomainModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::getUpdatedComics $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getNewestComics(page: Int?): Either<ComicAppError, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .getNewestComics(page)
          .map(Mapper::responseToDomainModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::getNewestComics $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getComicDetail(comicLink: String): Either<ComicAppError, ComicDetail> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .getComicDetail(comicLink)
          .let(Mapper::responseToDomainModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::getComicDetail $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getChapterDetail(chapterLink: String): Either<ComicAppError, ChapterDetail> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .getChapterDetail(chapterLink)
          .let(Mapper::responseToDomainModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::getDetail $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getAllCategories(): Either<ComicAppError, List<Category>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .getAllCategories()
          .map(Mapper::responseToDomainModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::getAllCategories $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun searchComic(query: String, page: Int?): Either<ComicAppError, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .searchComic(query, page)
          .map(Mapper::responseToDomainModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "ComicRepositoryImpl::searchComic $throwable")
      throwable.toError(retrofit).left()
    }
  }
}