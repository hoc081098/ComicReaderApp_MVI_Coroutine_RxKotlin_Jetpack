@file:Suppress("NOTHING_TO_INLINE")

package com.hoc.comicapp.utils

import arrow.core.Either
import arrow.core.getOrHandle

// TODO: using monad comprehension
inline fun <L : Throwable, R> Either<L, R>.getOrThrow(): R = getOrHandle { throw it }

