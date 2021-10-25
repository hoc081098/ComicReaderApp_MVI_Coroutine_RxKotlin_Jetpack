package com.hoc.comicapp.data

import android.database.SQLException
import arrow.core.nonFatalOrThrow
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.storage.StorageException
import com.hoc.comicapp.data.firebase.entity._FavoriteComic
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import com.hoc.comicapp.data.local.entities.ComicEntity
import com.hoc.comicapp.data.remote.ErrorResponseParser
import com.hoc.comicapp.data.remote.response.CategoryDetailPopularComicResponse
import com.hoc.comicapp.data.remote.response.CategoryResponse
import com.hoc.comicapp.data.remote.response.ChapterDetailResponse
import com.hoc.comicapp.data.remote.response.ComicDetailResponse
import com.hoc.comicapp.data.remote.response.ComicResponse
import com.hoc.comicapp.domain.models.AuthError
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.CategoryDetailPopularComic
import com.hoc.comicapp.domain.models.ChapterDetail
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.models.LocalStorageError
import com.hoc.comicapp.domain.models.NetworkError
import com.hoc.comicapp.domain.models.ServerError
import com.hoc.comicapp.domain.models.UnexpectedError
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object Mappers {

  /**
   *
   */

  fun domainToFirebaseEntity(comic: FavoriteComic): _FavoriteComic {
    return _FavoriteComic(
      url = comic.url,
      title = comic.title,
      thumbnail = comic.thumbnail,
      view = comic.view,
      createdAt = null
    )
  }

  fun domainToLocalEntity(domain: DownloadedChapter): ChapterEntity {
    return ChapterEntity(
      chapterLink = domain.chapterLink,
      comicLink = domain.comicLink,
      downloadedAt = domain.downloadedAt,
      images = domain.images,
      time = domain.time,
      view = domain.view,
      chapterName = domain.chapterName,
      order = -1
    )
  }

  fun domainToLocalEntity(domain: DownloadedComic): ComicEntity {
    return ComicEntity(
      comicLink = domain.comicLink,
      view = domain.view,
      categories = domain.categories.map {
        ComicEntity.Category(
          link = it.link,
          name = it.name
        )
      },
      authors = domain.authors.map {
        ComicEntity.Author(
          link = it.link,
          name = it.name
        )
      },
      thumbnail = domain.thumbnail,
      lastUpdated = domain.lastUpdated,
      shortenedContent = domain.shortenedContent,
      title = domain.title,
      remoteThumbnail = domain.thumbnail
    )
  }

  /**
   *
   */

  fun entityToDomainModel(entity: ChapterEntity): DownloadedChapter {
    return DownloadedChapter(
      chapterName = entity.chapterName,
      view = entity.view,
      time = entity.time,
      chapterLink = entity.chapterLink,
      images = entity.images,
      downloadedAt = entity.downloadedAt,
      comicLink = entity.comicLink,
      prevChapterLink = null,
      nextChapterLink = null,
      chapters = emptyList()
    )
  }

  fun entityToDomainModel(entity: ComicAndChapters): DownloadedComic {
    val comic = entity.comic
    val chapters = entity.chapters

    return DownloadedComic(
      title = comic.title,
      view = comic.view,
      comicLink = comic.comicLink,
      lastUpdated = comic.lastUpdated,
      shortenedContent = comic.shortenedContent,
      thumbnail = comic.thumbnail,
      authors = comic.authors.map {
        DownloadedComic.Author(
          name = it.name,
          link = it.link
        )
      },
      categories = comic.categories.map {
        DownloadedComic.Category(
          name = it.name,
          link = it.link
        )
      },
      chapters = chapters.map {
        DownloadedChapter(
          chapterName = it.chapterName,
          comicLink = it.comicLink,
          view = it.view,
          downloadedAt = it.downloadedAt,
          chapterLink = it.chapterLink,
          images = it.images,
          time = it.time,
          prevChapterLink = null,
          nextChapterLink = null,
          chapters = emptyList()
        )
      },
      remoteThumbnail = comic.remoteThumbnail
    )
  }

  /**
   *
   */

  fun responseToDomainModel(response: ComicResponse): Comic {
    return Comic(
      title = response.title,
      thumbnail = response.thumbnail,
      link = response.link,
      view = response.view,
      lastChapters = response.lastChapters.map {
        Comic.LastChapter(
          chapterLink = it.chapterLink,
          chapterName = it.chapterName,
          time = it.time
        )
      }
    )
  }

  fun responseToDomainModel(response: ComicDetailResponse): ComicDetail {
    return ComicDetail(
      link = response.link,
      thumbnail = response.thumbnail,
      view = response.view,
      title = response.title,
      chapters = response.chapters.map {
        ComicDetail.Chapter(
          chapterLink = it.chapterLink,
          view = it.view,
          time = it.time,
          chapterName = it.chapterName
        )
      },
      authors = response.authors.map {
        ComicDetail.Author(
          link = it.link,
          name = it.name
        )
      },
      categories = response.categories.map {
        ComicDetail.Category(
          link = it.link,
          name = it.name
        )
      },
      lastUpdated = response.lastUpdated,
      shortenedContent = response.shortenedContent,
      relatedComics = response.relatedComics.map(::responseToDomainModel)
    )
  }

  fun responseToDomainModel(response: ChapterDetailResponse): ChapterDetail {
    return ChapterDetail(
      chapterName = response.chapterName,
      chapterLink = response.chapterLink,
      images = response.images,
      chapters = response.chapters.map {
        ChapterDetail.Chapter(
          chapterLink = it.chapterLink,
          chapterName = it.chapterName
        )
      },
      nextChapterLink = response.nextChapterLink,
      prevChapterLink = response.prevChapterLink
    )
  }

  fun responseToDomainModel(response: CategoryResponse): Category {
    return Category(
      link = response.link,
      name = response.name,
      description = response.description,
      thumbnail = response.thumbnail
    )
  }

  fun responseToDomainModel(response: CategoryDetailPopularComicResponse): CategoryDetailPopularComic {
    return CategoryDetailPopularComic(
      title = response.title,
      thumbnail = response.thumbnail,
      link = response.link,
      lastChapter = CategoryDetailPopularComic.LastChapter(
        chapterName = response.lastChapter.chapterName,
        chapterLink = response.lastChapter.chapterLink
      )
    )
  }

  fun responseToLocalEntity(response: ComicDetailResponse): ComicEntity {
    return ComicEntity(
      authors = response.authors.map {
        ComicEntity.Author(
          link = it.link,
          name = it.name
        )
      },
      categories = response.categories.map {
        ComicEntity.Category(
          link = it.link,
          name = it.name
        )
      },
      lastUpdated = response.lastUpdated,
      comicLink = response.link,
      shortenedContent = response.shortenedContent,
      thumbnail = response.thumbnail,
      title = response.title,
      view = response.view,
      remoteThumbnail = response.thumbnail
    )
  }

  fun responseToFirebaseEntity(response: ComicResponse): _FavoriteComic {
    return _FavoriteComic(
      url = response.link,
      thumbnail = response.thumbnail,
      createdAt = null,
      view = response.view,
      title = response.title
    )
  }

  fun responseToFirebaseEntity(response: ComicDetailResponse): _FavoriteComic {
    return _FavoriteComic(
      url = response.link,
      thumbnail = response.thumbnail,
      createdAt = null,
      view = response.view,
      title = response.title
    )
  }
}

class ErrorMapper(private val retrofit: Retrofit) : (Throwable) -> ComicAppError {
  /**
   * Transform [t] to [ComicAppError].
   * Throw [t] if fatal.
   */
  override fun invoke(t: Throwable): ComicAppError {
    val throwable = t.nonFatalOrThrow()
    return runCatching {
      when (throwable) {
        is ComicAppError -> throwable
        is FirebaseException -> mapFirebaseException(throwable)
        is SQLException -> LocalStorageError.DatabaseError(throwable)
        is IOException -> mapIOException(throwable)
        is HttpException -> mapHttpException(throwable)
        else -> UnexpectedError(
          cause = throwable,
          message = "Unknown throwable $throwable"
        )
      }
    }.getOrElse {
      it.nonFatalOrThrow()
      UnexpectedError(
        cause = it,
        message = "Unknown throwable $it"
      )
    }
  }

  private fun mapHttpException(throwable: HttpException): ComicAppError {
    return ErrorResponseParser
      .getError(
        throwable.response() ?: return ServerError("Response is null", -1),
        retrofit
      )
      ?.let {
        ServerError(
          message = it.message,
          statusCode = it.statusCode
        )
      }
      ?: ServerError("Parsed error response is null", -1)
  }
}

private fun mapFirebaseException(fbException: FirebaseException) = when (fbException) {
  is FirebaseNetworkException -> NetworkError
  is FirebaseAuthException -> {
    when (fbException.errorCode) {
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
        cause = fbException,
        message = "Unknown throwable $fbException"
      )
    }
  }
  is StorageException -> AuthError.UploadFile
  else -> UnexpectedError(
    cause = fbException,
    message = "Unknown throwable $fbException"
  )
}

private fun mapIOException(ioException: IOException) = when (ioException) {
  is UnknownHostException -> NetworkError
  is SocketTimeoutException -> NetworkError
  else -> UnexpectedError(
    cause = ioException,
    message = "Unknown IOException $ioException"
  )
}
