package com.hoc.comicapp.domain.models

import android.database.sqlite.SQLiteException
import com.hoc.comicapp.data.remote.ErrorResponseParser
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class ComicAppError : Throwable()

object NetworkError : ComicAppError()

sealed class LocalStorageError : ComicAppError() {
  object DeleteFileError : LocalStorageError()
  object SaveFileError : LocalStorageError()
  data class DatabaseError(override val cause: Throwable? = null) : LocalStorageError()
}

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
    is SQLiteException -> LocalStorageError.DatabaseError(this)
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


fun ComicAppError.getMessage(): String {
  return when (this) {
    NetworkError -> "Network error"
    is ServerError -> "Server error: $message"
    is UnexpectedError -> "An unexpected error occurred"
    LocalStorageError.DeleteFileError -> "Error when deleting file"
    LocalStorageError.SaveFileError -> "Error when saving file"
    is LocalStorageError.DatabaseError -> "Database error"
  }
}