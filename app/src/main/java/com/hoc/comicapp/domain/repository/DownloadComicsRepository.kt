package com.hoc.comicapp.domain.repository

import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.utils.Either

interface DownloadComicsRepository {
  suspend fun downloadChapter(chapterLink: String): Either<ComicAppError, Unit>
}