package com.hoc.comicapp.data.models

import com.hoc.comicapp.data.remote.ErrorResponseParser
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class ComicAppError : Throwable()

object NetworkError : ComicAppError()

data class ServerError(
  override val message: String,
  val statusCode: Int
) : ComicAppError()

data class UnexpectedError(
  override val message: String,
  override val cause: Throwable?
) : ComicAppError()

fun Throwable.toError(retrofit: Retrofit): ComicAppError {
  return when (this) {
    is IOException -> when (this) {
      is UnknownHostException -> NetworkError
      is SocketTimeoutException -> NetworkError
      else -> UnexpectedError(
        cause = this,
        message = "Unknown IOException $this"
      )
    }
    is HttpException -> ErrorResponseParser
      .getError(
        response() ?: return ServerError("Response is null", -1),
        retrofit
      )
      ?.let {
        ServerError(
          message = it.message,
          statusCode = it.statusCode
        )
      } ?: ServerError("", -1)
    else -> UnexpectedError(
      cause = this,
      message = "Unknown throwable $this"
    )
  }
}


fun ComicAppError.getMessageFromError(): String {
  return when (this) {
    NetworkError -> "Network error"
    is ServerError -> "Server error: $message"
    is UnexpectedError -> "An unexpected error occurred"
  }
}