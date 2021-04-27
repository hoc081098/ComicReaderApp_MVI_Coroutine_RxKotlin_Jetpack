package com.hoc.comicapp.utils

import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import java.io.InputStream

fun InputStream.copyTo(
  target: File,
  overwrite: Boolean = false,
  bufferSize: Int = DEFAULT_BUFFER_SIZE,
): File {
  if (target.exists()) {
    val stillExists = if (!overwrite) true else !target.delete()

    if (stillExists) {
      throw IllegalAccessException("The destination file already exists.")
    }
  }

  target.parentFile?.mkdirs()

  this.use { input ->
    target.outputStream().use { output ->
      input.copyTo(output, bufferSize)
    }
  }

  return target
}

suspend fun <T> retryIO(
  times: Int,
  initialDelay: Long,
  factor: Double,
  maxDelay: Long = Long.MAX_VALUE,
  block: suspend () -> T,
): T {
  var currentDelay = initialDelay
  repeat(times - 1) {
    try {
      return block()
    } catch (e: IOException) {
      // you can log an error here and/or make a more finer-grained
      // analysis of the cause to see if retry is needed
    }
    delay(currentDelay)
    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
  }
  return block() // last attempt
}
