package com.hoc.comicapp.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import io.reactivex.Observable

@Dao
abstract class ChapterDao {
  @Query("SELECT * FROM downloaded_chapters WHERE chapter_link = :chapterLink")
  abstract fun getByChapterLink(chapterLink: String): Observable<ChapterEntity>

  @Query("SELECT COUNT(*) FROM downloaded_chapters WHERE comic_link = :comicLink")
  abstract suspend fun getCountByComicLink(comicLink: String): List<Int>

  /**
   * This query will tell Room to query both the [ComicEntity] and [ChapterEntity] tables and handle
   * the object mapping.
   */
  @Transaction
  @Query("SELECT * FROM downloaded_comics")
  abstract fun getComicAndChapters(): Observable<List<ComicAndChapters>>

  @Transaction
  @Query("SELECT * FROM downloaded_chapters")
  abstract fun getAllChapters(): LiveData<List<ChapterEntity>>

  @Insert(onConflict = IGNORE)
  abstract suspend fun insert(chapter: ChapterEntity): Long

  @Delete
  abstract suspend fun delete(chapter: ChapterEntity)

  @Update
  abstract suspend fun update(chapter: ChapterEntity)

  @Query("DELETE FROM downloaded_chapters WHERE comic_link = :comicLink")
  abstract suspend fun deleteAllByComicLink(comicLink: Long)

  @Transaction
  open suspend fun upsert(chapter: ChapterEntity) {
    insert(chapter)
      .takeIf { it == -1L }
      ?.let { update(chapter) }
  }
}