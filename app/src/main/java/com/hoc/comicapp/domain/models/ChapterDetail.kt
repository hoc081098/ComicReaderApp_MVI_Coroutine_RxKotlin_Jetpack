package com.hoc.comicapp.domain.models

data class ChapterDetail(
  val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/cu-dam-huy-diet/chap-153/474661
  val chapterName: String, // Chapter 153
  val images: List<String>,
  val time: String, // 16:27 04/06/2019
  val htmlContent: String?,
  val prevChapterLink: String?,
  val nextChapterLink: String?,
  val chapters: List<Chapter>
) {
  data class Chapter(
    val chapterName: String,
    val chapterLink: String
  )
}