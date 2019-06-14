package com.hoc.comicapp.domain.models

data class UpdatedComic(
  val lastChapters: List<Chapter>,
  val link: String, // http://www.nettruyen.com/truyen-tranh/em-den-tu-noi-dai-duong-vinh-hang
  val thumbnail: String, // http://st.nettruyen.com/data/comics/156/em-den-tu-noi-dai-duong-vinh-hang.jpg
  val title: String, // Em Đến Từ Nơi Đại Dương Vĩnh Hằng
  val view: String? // 132.737
) {
  data class Chapter(
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/em-den-tu-noi-dai-duong-vinh-hang/chap-16.5/475020
    val chapterName: String, // Chapter 16.5
    val time: String // 18:02 05/06
  )
}