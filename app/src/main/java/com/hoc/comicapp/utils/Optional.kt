package com.hoc.comicapp.utils

import java.util.*

sealed class Optional<out T>

data class Some<T : Any>(val value: T) : Optional<T>()

object None : Optional<Nothing>() {
  override fun toString() = "None"
}

fun <T, R : Any> Optional<T>.map(transform: (T) -> R): Optional<R> = when (this) {
  is Some -> Some(transform(value))
  is None -> None
}

fun <T : Any> T?.toOptional(): Optional<T> = when (this) {
  null -> None
  else -> Some(this)
}

fun <T> Optional<T>.getOrNull(): T? = when (this) {
  is Some -> value
  else -> null
}

fun <T> Optional<T>.getOrThrow(): T = when (this) {
  is Some -> value
  else -> throw NoSuchElementException("No value present")
}