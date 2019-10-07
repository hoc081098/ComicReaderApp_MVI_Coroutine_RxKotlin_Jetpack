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

  fun getDownloadedComics(): Observable<Either<ComicAppError, List<DownloadedComic>>>

  fun getDownloadedComic(link: String): Observable<Either<ComicAppError, DownloadedComic>>

  fun getDownloadedChapters() : LiveData<List<DownloadedChapter>>

  suspend fun deleteDownloadedChapter(chapter: DownloadedChapter): Either<ComicAppError, Unit>

  suspend fun deleteComic(comic: DownloadedComic): Either<ComicAppError, Unit>
}