package com.hoc.comicapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChapterDao {
  @Query("SELECT * FROM downloaded_chapters WHERE chapter_link = :chapterLink")
  abstract fun getByChapterLink(chapterLink: String): Flow<ChapterEntity>

  @Query("SELECT COUNT(*) FROM downloaded_chapters WHERE comic_link = :comicLink")
  abstract suspend fun getCountByComicLink(comicLink: String): List<Int>

  @Query("SELECT * FROM downloaded_chapters")
  abstract fun getAllChaptersFlow(): Flow<List<ChapterEntity>>

  @Insert(onConflict = IGNORE)
  abstract suspend fun insert(chapter: ChapterEntity): Long

  @Delete
  abstract suspend fun delete(chapter: ChapterEntity)

  @Update
  abstract suspend fun update(chapter: ChapterEntity)

  @Transaction
  open suspend fun upsert(chapter: ChapterEntity) {
    insert(chapter)
      .takeIf { it == -1L }
      ?.let { update(chapter) }
  }

  /**
   * This query will tell Room to query both the [com.hoc.comicapp.data.local.entities.ComicEntity]
   * and [ChapterEntity] tables and handle
   * the object mapping.
   */
  @Transaction
  @Query("SELECT * FROM downloaded_comics")
  abstract fun getComicAndChapters(): Observable<List<ComicAndChapters>>
}
