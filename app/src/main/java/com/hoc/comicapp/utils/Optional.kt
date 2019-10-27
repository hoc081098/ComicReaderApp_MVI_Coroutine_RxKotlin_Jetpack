package com.hoc.comicapp.utils

import java.util.*

sealed class Optional<out T>

data class Some<T : Any>(val value: T) : Optional<T>()

object None : Optional<Nothing>() {
  override fun toString() = "None"
}

inline fun <T, R : Any> Optional<T>.map(transform: (T) -> R): Optional<R> = when (this) {
  is Some -> Some(transform(value))
  is None -> None
}

/**
 *
 */

inline fun <T, R> Optional<T>.fold(ifEmpty: () -> R, ifSome: (T) -> R): R = when (this) {
  is None -> ifEmpty()
  is Some -> ifSome(value)
}

inline fun <T> Optional<T>.getOrElse(ifNone: () -> T) = fold(ifNone) { it }

fun <T> Optional<T>.getOrNull(): T? = getOrElse { null }

fun <T> Optional<T>.getOrThrow(): T = getOrElse { throw NoSuchElementException("No value present") }

/**
 *
 */

fun <T : Any> T?.toOptional(): Optional<T> = when (this) {
  null -> None
  else -> Some(this)
}
