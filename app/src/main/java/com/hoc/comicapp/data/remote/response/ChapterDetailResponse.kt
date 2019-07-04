package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class ChapterDetailResponse(
  @Json(name = "chapter_link")
  val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/cu-dam-huy-diet/chap-153/474661
  @Json(name = "chapter_name")
  val chapterName: String, // Chapter 153
  @Json(name = "images")
  val images: List<String>,
  @Json(name = "time")
  val time: String, // 16:27 04/06/2019
  @Json(name = "html_content")
  val htmlContent: String?,
  @Json(name = "prev_chapter_link")
  val prevChapterLink: String?,
  @Json(name = "next_chapter_link")
  val nextChapterLink: String?,
  @Json(name = "chapters")
  val chapters: List<Chapter>
) {
  data class Chapter(
    @Json(name = "chapter_name")
    val chapterName: String,
    @Json(name = "chapter_link")
    val chapterLink: String
  )
}