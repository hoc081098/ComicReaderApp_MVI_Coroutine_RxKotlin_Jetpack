package com.hoc.comicapp.domain.models

data class SuggestComic(
  val lastChapter: Chapter,
  val link: String, // http://www.nettruyen.com/truyen-tranh/vuong-gia-khong-nen-a
  val thumbnail: String, // https://3.bp.blogspot.com/-XR_B6tlKrpw/Wo6kMfpEdjI/AAAAAAAANUU/tYdlCVB0DCcOmJ9nWxZ3hUs6BomPIVY6QCHMYCw/vuong-gia-khong-nen-a
  val title: String // Vương Gia ! Không nên a !
) {
  data class Chapter(
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/vuong-gia-khong-nen-a/chap-194/477849
    val chapterName: String, // Chapter 194
    val time: String // 1 ngày trước
  )
}