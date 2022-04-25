package com.hoc.comicapp.data.remote.response

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class ChapterDetailResponse(
  @Json(name = "chapter_link")
  val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-65-159349729567655
  @Json(name = "chapter_name")
  val chapterName: String, // Chapter 65
  @Json(name = "chapters")
  val chapters: List<Chapter>,
  @Json(name = "images")
  val images: List<String>,
  @Json(name = "comic_name")
  val comicName: String,
  @Json(name = "comic_link")
  val comicLink: String,
  @Json(name = "prev_chapter_link")
  val prevChapterLink: String?, // https://ww2.mangafox.online/solo-leveling/chapter-64-1137969534934196
  @Json(name = "next_chapter_link")
  val nextChapterLink: String?,
) {
  @Keep
  data class Chapter(
    @Json(name = "chapter_link")
    val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-0-275968490470920
    @Json(name = "chapter_name")
    val chapterName: String, // Chapter 0
  )
}
