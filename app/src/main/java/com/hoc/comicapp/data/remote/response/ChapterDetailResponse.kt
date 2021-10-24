package com.hoc.comicapp.data.remote.response

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class ChapterDetailResponse(
  @field:Json(name = "chapter_link")
  val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-65-159349729567655
  @field:Json(name = "chapter_name")
  val chapterName: String, // Chapter 65
  @field:Json(name = "chapters")
  val chapters: List<Chapter>,
  @field:Json(name = "images")
  val images: List<String>,
  @field:Json(name = "comic_name")
  val comicName: String,
  @field:Json(name = "comic_link")
  val comicLink: String,
  @field:Json(name = "prev_chapter_link")
  val prevChapterLink: String?, // https://ww2.mangafox.online/solo-leveling/chapter-64-1137969534934196
  @field:Json(name = "next_chapter_link")
  val nextChapterLink: String?,
) {
  @Keep
  data class Chapter(
    @field:Json(name = "chapter_link")
    val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-0-275968490470920
    @field:Json(name = "chapter_name")
    val chapterName: String, // Chapter 0
  )
}
