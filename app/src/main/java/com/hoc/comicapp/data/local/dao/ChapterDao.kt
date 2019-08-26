package com.hoc.comicapp.data.local.dao

import androidx.room.*
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import io.reactivex.Observable

@Dao
abstract class ChapterDao {
  @Query("SELECT * FROM downloaded_chapters WHERE chapter_link = :chapterLink")
  abstract fun getByChapterLink(chapterLink: String): Observable<ChapterEntity>

  @Query("SELECT * FROM downloaded_chapters WHERE comic_link = :comicLink ORDER BY `order` DESC")
  abstract fun getAllByComicLink(comicLink: Long): Observable<List<ChapterEntity>>

  /**
   * This query will tell Room to query both the [ComicEntity] and [ChapterEntity] tables and handle
   * the object mapping.
   */
  @Transaction
  @Query("SELECT * FROM downloaded_comics")
  abstract fun getComicAndChapters(): Observable<List<ComicAndChapters>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun insert(chapters: List<ChapterEntity>)

  @Delete
  abstract fun delete(chapter: ChapterEntity)

  @Query("DELETE FROM downloaded_chapters WHERE comic_link = :comicLink")
  abstract fun deleteAllByComicLink(comicLink: Long)

}