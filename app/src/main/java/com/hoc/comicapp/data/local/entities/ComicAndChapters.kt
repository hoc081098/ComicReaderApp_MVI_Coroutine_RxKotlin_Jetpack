package com.hoc.comicapp.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * This class captures the relationship between a [ComicEntity] and a comic's [ChapterEntity]s, which is
 * used by Room to fetch the related entities.
 */
class ComicAndChapters {
  @Embedded
  lateinit var comic: ComicEntity

  @Relation(parentColumn = "comic_link", entityColumn = "comic_link")
  lateinit var chapters: List<ChapterEntity>
}
