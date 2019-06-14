package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class TopMonthComicResponse(
  @Json(name = "last_chapter")
  val lastChapter: Chapter,
  @Json(name = "link")
  val link: String, // http://www.nettruyen.com/truyen-tranh/toan-chuc-phap-su
  @Json(name = "thumbnail")
  val thumbnail: String, // https://3.bp.blogspot.com/-1YeiZW1XZz0/Wo9lPoYCCCI/AAAAAAAANaQ/3yN0TeIWFt0D9PB4pIGXPCpI69MoWKmIgCHMYCw/toan-chuc-phap-su
  @Json(name = "title")
  val title: String, // Toàn Chức Pháp Sư
  @Json(name = "view")
  val view: String? // 2.888.130
) {
  data class Chapter(
    @Json(name = "chapter_link")
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/toan-chuc-phap-su/chap-296/472537
    @Json(name = "chapter_name")
    val chapterName: String // Chapter 296
  )
}