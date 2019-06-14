package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class SuggestComicResponse(
  @Json(name = "last_chapter")
  val lastChapter: Chapter,
  @Json(name = "link")
  val link: String, // http://www.nettruyen.com/truyen-tranh/vuong-gia-khong-nen-a
  @Json(name = "thumbnail")
  val thumbnail: String, // https://3.bp.blogspot.com/-XR_B6tlKrpw/Wo6kMfpEdjI/AAAAAAAANUU/tYdlCVB0DCcOmJ9nWxZ3hUs6BomPIVY6QCHMYCw/vuong-gia-khong-nen-a
  @Json(name = "title")
  val title: String // Vương Gia ! Không nên a !
) {
  data class Chapter(
    @Json(name = "chapter_link")
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/vuong-gia-khong-nen-a/chap-194/477849
    @Json(name = "chapter_name")
    val chapterName: String, // Chapter 194
    @Json(name = "time")
    val time: String // 1 ngày trước
  )
}