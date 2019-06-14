package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class UpdatedComicResponse(
  @Json(name = "last_chapters")
  val lastChapters: List<Chapter>,
  @Json(name = "link")
  val link: String, // http://www.nettruyen.com/truyen-tranh/em-den-tu-noi-dai-duong-vinh-hang
  @Json(name = "thumbnail")
  val thumbnail: String, // http://st.nettruyen.com/data/comics/156/em-den-tu-noi-dai-duong-vinh-hang.jpg
  @Json(name = "title")
  val title: String, // Em Đến Từ Nơi Đại Dương Vĩnh Hằng
  @Json(name = "view")
  val view: String? // 132.737
) {
  data class Chapter(
    @Json(name = "chapter_link")
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/em-den-tu-noi-dai-duong-vinh-hang/chap-16.5/475020
    @Json(name = "chapter_name")
    val chapterName: String, // Chapter 16.5
    @Json(name = "time")
    val time: String // 18:02 05/06
  )
}