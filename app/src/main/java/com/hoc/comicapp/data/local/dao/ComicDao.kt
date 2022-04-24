package com.hoc.comicapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.IGNORE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import com.hoc.comicapp.data.local.entities.ComicEntity
import io.reactivex.rxjava3.core.Observable

@Dao
abstract class ComicDao {
  @Transaction
  @Query("SELECT * FROM downloaded_comics WHERE comic_link = :comicLink")
  abstract fun getByComicLink(comicLink: String): Observable<ComicAndChapters>

  @Query("SELECT * FROM downloaded_comics WHERE comic_link = :comicLink")
  abstract suspend fun findByComicLink(comicLink: String): ComicEntity?

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
