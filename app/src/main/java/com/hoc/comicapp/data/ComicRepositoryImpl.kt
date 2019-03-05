package com.hoc.comicapp.data

import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.ComicAppError
import com.hoc.comicapp.data.models.toError
import com.hoc.comicapp.data.remote.ComicApiService
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
  override suspend fun getTopMonth(): Either<ComicAppError, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .topMonth()
          .await()
          .map { Mapper.comicResponseToComicModel(it) }
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "getTopMonth $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getUpdate(page: Int?): Either<ComicAppError, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .update(page = page)
          .await()
          .map { Mapper.comicResponseToComicModel(it) }
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "getUpdate $throwable")
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getSuggest(): Either<ComicAppError, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .suggest()
          .await()
          .map { Mapper.comicResponseToComicModel(it) }
          .right()
      }
    } catch (throwable: Throwable) {
      Timber.d(throwable, "getSuggest $throwable")
      throwable.toError(retrofit).left()
    }
  }
}