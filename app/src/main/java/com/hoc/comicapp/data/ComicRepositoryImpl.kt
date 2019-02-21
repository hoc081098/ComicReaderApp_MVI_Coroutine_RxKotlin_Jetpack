package com.hoc.comicapp.data

import android.util.Log
import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.Either
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.Error
import com.hoc.comicapp.data.models.toError
import com.hoc.comicapp.data.remote.ComicApiService
import com.hoc.comicapp.left
import com.hoc.comicapp.right
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

class ComicRepositoryImpl(
  private val retrofit: Retrofit,
  private val comicApiService: ComicApiService,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : ComicRepository {
  override suspend fun getTopMonth(): Either<Error, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .topMonth()
          .await()
          .map(Mapper::comicResponseToComicModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Log.d("@@@", "getTopMonth $throwable", throwable)
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getUpdate(page: Int?): Either<Error, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .update(page = page)
          .await()
          .map(Mapper::comicResponseToComicModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Log.d("@@@", "getUpdate $throwable", throwable)
      throwable.toError(retrofit).left()
    }
  }

  override suspend fun getSuggest(): Either<Error, List<Comic>> {
    return try {
      withContext(dispatcherProvider.io) {
        comicApiService
          .suggest()
          .await()
          .map(Mapper::comicResponseToComicModel)
          .right()
      }
    } catch (throwable: Throwable) {
      Log.d("@@@", "getSuggest $throwable", throwable)
      throwable.toError(retrofit).left()
    }
  }
}