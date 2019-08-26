package com.hoc.comicapp.data.local.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import com.hoc.comicapp.data.local.entities.ComicEntity
import io.reactivex.Observable

@Dao
abstract class ComicDao {
  @Query("SELECT * FROM downloaded_comics WHERE comic_link = :comicLink")
  abstract fun getByComicLink(comicLink: String): Observable<ComicEntity>

  @Insert(onConflict = IGNORE)
  abstract fun insert(comic: ComicEntity): Long

  @Update
  abstract fun update(comic: ComicEntity)

  @Delete
  abstract fun delete(comic: ComicEntity)

  @Transaction
  open fun upsert(comic: ComicEntity) {
    insert(comic)
      .takeIf { it == -1L }
      ?.let { update(comic) }
  }
}