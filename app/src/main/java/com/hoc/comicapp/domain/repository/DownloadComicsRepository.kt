package com.hoc.comicapp.domain.repository

import androidx.lifecycle.LiveData
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.utils.Either
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow

interface DownloadComicsRepository {
  fun downloadChapter(chapterLink: String): Flow<Int>

  fun downloadedComics(): Observable<Either<ComicAppError, List<DownloadedComic>>>

  fun downloadedChapters() : LiveData<List<DownloadedChapter>>
}