package com.hoc.comicapp.data

import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.ComicAppError

interface ComicRepository {
  suspend fun getTopMonth(): Either<ComicAppError, List<Comic>>

  suspend fun getUpdate(page: Int? = null): Either<ComicAppError, List<Comic>>

  suspend fun getSuggest(): Either<ComicAppError, List<Comic>>
}