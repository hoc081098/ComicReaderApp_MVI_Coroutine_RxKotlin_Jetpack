package com.hoc.comicapp.domain.repository

import androidx.lifecycle.LiveData
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.utils.Either
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow

interface DownloadComicsRepository {
  /**
   * Download chapter by [chapterLink]
   * @param chapterLink chapter url
   * @return Flow of progress (from 0 to 100)
   */
  fun downloadChapter(chapterLink: String): Flow<Int>

  /**
   * Get all downloaded comics
   * @return observable
   */
  fun getDownloadedComics(): Observable<Either<ComicAppError, List<DownloadedComic>>>

  /**
   * Get downloaded comic by [link]
   * @return observable
   */
  fun getDownloadedComic(link: String): Observable<Either<ComicAppError, DownloadedComic>>

  /**
   * Get all downloaded chapters
   * @return live data
   */
  fun getDownloadedChapters(): LiveData<List<DownloadedChapter>>

  /**
   * Delete downloaded [chapter]
   * @return either
   */
  suspend fun deleteDownloadedChapter(chapter: DownloadedChapter): Either<ComicAppError, Unit>

  /**
   * Delete downloaded [comic]
   * @return either
   */
  suspend fun deleteComic(comic: DownloadedComic): Either<ComicAppError, Unit>

  /**
   * Enqueue download chapter worker
   * @param chapter
   * @param comicName
   * @return either
   */
  suspend fun enqueueDownload(
    chapter: DownloadedChapter,
    comicName: String
  ): Either<ComicAppError, Unit>

  fun getDownloadedChapter(chapterLink: String): Flow<Either<ComicAppError, DownloadedChapter>>
}