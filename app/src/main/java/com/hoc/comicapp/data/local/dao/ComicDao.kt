package com.hoc.comicapp.data.local.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import com.hoc.comicapp.data.local.entities.ComicEntity
import io.reactivex.Observable

@Dao
abstract class ComicDao {
  @Transaction
  @Query("SELECT * FROM downloaded_comics WHERE comic_link = :comicLink")
  abstract fun getByComicLink(comicLink: String): Observable<ComicAndChapters>

  @Insert(onConflict = IGNORE)
  abstract suspend fun insert(comic: ComicEntity): Long

  @Update
  abstract suspend fun update(comic: ComicEntity)

  @Delete
  abstract suspend fun delete(comic: ComicEntity)

  @Transaction
  open suspend fun upsert(comic: ComicEntity) {
    insert(comic)
      .takeIf { it == -1L }
      ?.let { update(comic) }
  }
}