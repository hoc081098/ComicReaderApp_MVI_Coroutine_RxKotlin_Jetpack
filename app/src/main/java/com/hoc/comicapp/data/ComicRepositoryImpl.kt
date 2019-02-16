package com.hoc.comicapp.data

import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.remote.ComicApiService
import kotlinx.coroutines.withContext

class ComicRepositoryImpl(
  private val comicApiService: ComicApiService,
  private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ComicRepository {
  override suspend fun getTopMonth(): List<Comic> {
    return withContext(coroutinesDispatcherProvider.io) {
      comicApiService
        .topMonth()
        .await()
        .map(Mapper::comicResponseToComicModel)
    }
  }

  override suspend fun getUpdate(page: Int?): List<Comic> {
    return withContext(coroutinesDispatcherProvider.io) {
      comicApiService
        .update(page = page)
        .await()
        .map(Mapper::comicResponseToComicModel)
    }
  }

  override suspend fun getSuggest(): List<Comic> {
    return withContext(coroutinesDispatcherProvider.io) {
      comicApiService
        .suggest()
        .await()
        .map(Mapper::comicResponseToComicModel)
    }
  }
}