@file:Suppress("NOTHING_TO_INLINE")

package com.hoc.comicapp.utils

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.getOrHandle
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

inline fun <L : Throwable, R> Either<L, R>.getOrThrow(): R = getOrHandle { throw it }

suspend inline fun <A, B, C, L, R> parZipEither(
  ctx: CoroutineContext = EmptyCoroutineContext,
  crossinline fa: suspend () -> Either<L, A>,
  crossinline fb: suspend () -> Either<L, B>,
  crossinline fc: suspend () -> Either<L, C>,
  crossinline combiner: (A, B, C) -> R,
): Either<L, R> {
  return either {
    coroutineScope {
      val a = async(ctx) { fa().bind() }
      val b = async(ctx) { fb().bind() }
      val c = async(ctx) { fc().bind() }
      combiner(a.await(), b.await(), c.await())
    }
  }
}
