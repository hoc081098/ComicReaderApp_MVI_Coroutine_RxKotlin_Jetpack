package com.hoc.comicapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
  tableName = "downloaded_chapters",
  indices = [Index(value = ["comic_link"])],
  foreignKeys = [
    ForeignKey(
      entity = ComicEntity::class,
      onDelete = ForeignKey.CASCADE,
      onUpdate = ForeignKey.CASCADE,
      parentColumns = ["comic_link"],
      childColumns = ["comic_link"]
    )
  ]
)
data class ChapterEntity(
  @ColumnInfo(name = "chapter_link")
  @PrimaryKey
  val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-0-275968490470920

  @ColumnInfo(name = "chapter_name")
  val chapterName: String, // Chapter 0

  @ColumnInfo(name = "time")
  val time: String, // December 2018

  @ColumnInfo(name = "view")
  val view: String, // 9592

  @ColumnInfo(name = "images")
  val images: List<String>, // []

  @ColumnInfo(name = "comic_link")
  val comicLink: String,

  @ColumnInfo(name = "order")
  val order: Int,

  @ColumnInfo(name = "downloaded_at")
  val downloadedAt: Date,
)
