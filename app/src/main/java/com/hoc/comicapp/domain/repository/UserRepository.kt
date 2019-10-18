package com.hoc.comicapp.domain.repository

import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.utils.Either

interface UserRepository {
  suspend fun login(email: String, password: String): Either<ComicAppError, Unit>
}