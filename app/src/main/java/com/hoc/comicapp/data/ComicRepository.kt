package com.hoc.comicapp.data

import com.hoc.comicapp.Either
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.Error

interface ComicRepository {
  suspend fun getTopMonth(): Either<Error, List<Comic>>

  suspend fun getUpdate(page: Int? = null): Either<Error, List<Comic>>

  suspend fun getSuggest(): Either<Error, List<Comic>>
}