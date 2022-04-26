package com.hoc.comicapp.domain.repository

import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.DownloadProgress
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

interface DownloadComicsRepository {
  //region Query operations
  /**
   * Get all downloaded comics
   * @return observable
   */
  fun getDownloadedComics(): Observable<DomainResult<List<DownloadedComic>>>

  /**
   * Get downloaded comic by [link]
   * @return observable
   */
  fun getDownloadedComic(link: String): Observable<DomainResult<DownloadedComic>>

  /**
   * Get all downloaded chapters
   * @return live data
   */
  fun getDownloadedChapters(): Flow<List<DownloadedChapter>>

  /**
   * Get downloaded chapter by [chapterLink]
   * @return flow
   */
  fun getDownloadedChapter(chapterLink: String): Flow<DomainResult<DownloadedChapter>>
  //endregion

  //region Modification operations
  /**
   * Delete downloaded [chapter]
   * @return either
   */
  suspend fun deleteDownloadedChapter(chapter: DownloadedChapter): DomainResult<Unit>

  /**
   * Delete downloaded [comic]
   * @return either
   */
  suspend fun deleteComic(comic: DownloadedComic): DomainResult<Unit>

  /**
   * Enqueue download chapter worker
   * @param chapter
   * @param comicName
   * @return either
   */
  suspend fun enqueueDownload(
    chapter: DownloadedChapter,
    comicName: String,
    comicLink: String,
  ): DomainResult<Unit>

  /**
   * Download chapter by [chapterLink]
   * @param chapterLink chapter url
   * @return Flow of progress (from 0 to 100)
   */
  fun downloadChapter(chapterLink: String): Flow<DownloadProgress>
  //endregion
}
