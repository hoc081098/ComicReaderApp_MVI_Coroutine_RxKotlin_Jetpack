// ktlint-disable filename

package com.hoc.comicapp.domain

import arrow.core.Either
import com.hoc.comicapp.domain.models.ComicAppError

typealias DomainResult<T> = Either<ComicAppError, T>
