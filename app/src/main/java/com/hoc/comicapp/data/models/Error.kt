package com.hoc.comicapp.data.models

import com.hoc.comicapp.data.remote.ErrorResponseParser
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class Error : Throwable()

object NetworkError : Error()

data class ServerError(
  override val message: String,
  val statusCode: Int
) : Error()

data class UnexpectedError(
  override val message: String,
  override val cause: Throwable?
) : Error()

fun Throwable.toError(retrofit: Retrofit): Error {
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
      .getError(response(), retrofit)
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