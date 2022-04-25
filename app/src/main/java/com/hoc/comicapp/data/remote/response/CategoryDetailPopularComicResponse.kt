package com.hoc.comicapp.data.remote.response

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class CategoryDetailPopularComicResponse(
  @Json(name = "last_chapter")
  val lastChapter: LastChapter,
  @Json(name = "link")
  val link: String, // https://ww2.mangafox.online/the-fiancee-is-here
  @Json(name = "thumbnail")
  val thumbnail: String, // https://cdn1.mangafox.online/625/080/369/821/818/the-fiancee-is-here.jpg
  @Json(name = "title")
  val title: String, // The Fiancee is Here
) {
  @Keep
  data class LastChapter(
    @Json(name = "chapter_link")
    val chapterLink: String, // https://ww2.mangafox.online/the-fiancee-is-here/chapter-13-648903251249287
    @Json(name = "chapter_name")
    val chapterName: String, // Chapter 13
  )
}
