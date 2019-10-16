package com.hoc.comicapp.domain.models

import android.database.sqlite.SQLiteException
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
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

sealed class AuthError : ComicAppError() {
  object InvalidCustomToken : AuthError()
  object CustomTokenMismatch : AuthError()
  object InvalidCredential : AuthError()
  object InvalidEmail : AuthError()
  object WrongPassword : AuthError()
  object UserMismatch : AuthError()
  object RequiresRecentLogin : AuthError()
  object AccountExistsWithDifferenceCredential : AuthError()
  object EmailAlreadyInUse : AuthError()
  object CredentialAlreadyInUse : AuthError()
  object UserDisabled : AuthError()
  object TokenExpired : AuthError()
  object UserNotFound : AuthError()
  object InvalidUserToken : AuthError()
  object OperationNotAllowed : AuthError()
  object WeakPassword : AuthError()
}

fun Throwable.toError(retrofit: Retrofit): ComicAppError {
  return when (this) {
    is FirebaseException -> {
      when (this) {
        is FirebaseNetworkException -> NetworkError
        is FirebaseAuthException -> {
          when (this.errorCode) {
            "ERROR_INVALID_CUSTOM_TOKEN" -> AuthError.InvalidCustomToken
            "ERROR_CUSTOM_TOKEN_MISMATCH" -> AuthError.CustomTokenMismatch
            "ERROR_INVALID_CREDENTIAL" -> AuthError.InvalidCredential
            "ERROR_INVALID_EMAIL" -> AuthError.InvalidEmail
            "ERROR_WRONG_PASSWORD" -> AuthError.WrongPassword
            "ERROR_USER_MISMATCH" -> AuthError.UserMismatch
            "ERROR_REQUIRES_RECENT_LOGIN" -> AuthError.RequiresRecentLogin
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> AuthError.AccountExistsWithDifferenceCredential
            "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EmailAlreadyInUse
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> AuthError.CredentialAlreadyInUse
            "ERROR_USER_DISABLED" -> AuthError.UserDisabled
            "ERROR_USER_TOKEN_EXPIRED" -> AuthError.TokenExpired
            "ERROR_USER_NOT_FOUND" -> AuthError.UserNotFound
            "ERROR_INVALID_USER_TOKEN" -> AuthError.InvalidUserToken
            "ERROR_OPERATION_NOT_ALLOWED" -> AuthError.OperationNotAllowed
            "ERROR_WEAK_PASSWORD" -> AuthError.WeakPassword
            else -> UnexpectedError(
              cause = this,
              message = "Unknown throwable $this"
            )
          }
        }
        else -> UnexpectedError(
          cause = this,
          message = "Unknown throwable $this"
        )
      }
    }
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
    AuthError.InvalidCustomToken -> "Invalid custom token"
    AuthError.CustomTokenMismatch -> "Custom token mismatch"
    AuthError.InvalidCredential -> "Invalid credential"
    AuthError.InvalidEmail -> "Invalid email"
    AuthError.WrongPassword -> "Wrong password"
    AuthError.UserMismatch -> "User mismatch"
    AuthError.RequiresRecentLogin -> "Requires recent login"
    AuthError.AccountExistsWithDifferenceCredential -> "Account exists with difference credential"
    AuthError.EmailAlreadyInUse -> "Email already in use"
    AuthError.CredentialAlreadyInUse -> "Credential already in use"
    AuthError.UserDisabled -> "User disabled"
    AuthError.TokenExpired -> "Token expired"
    AuthError.UserNotFound -> "User not found"
    AuthError.InvalidUserToken -> "Invalid user token"
    AuthError.OperationNotAllowed -> "Operation not allowed"
    AuthError.WeakPassword -> "Weak password"
  }
}