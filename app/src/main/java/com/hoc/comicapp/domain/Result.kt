package com.hoc.comicapp.domain

import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.utils.Either

typealias DomainResult<T> = Either<ComicAppError, T>
