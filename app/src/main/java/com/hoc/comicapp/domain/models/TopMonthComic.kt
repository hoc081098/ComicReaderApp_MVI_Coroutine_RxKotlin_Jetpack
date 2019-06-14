package com.hoc.comicapp.domain.models

data class TopMonthComic(
  val lastChapter: Chapter,
  val link: String, // http://www.nettruyen.com/truyen-tranh/toan-chuc-phap-su
  val thumbnail: String, // https://3.bp.blogspot.com/-1YeiZW1XZz0/Wo9lPoYCCCI/AAAAAAAANaQ/3yN0TeIWFt0D9PB4pIGXPCpI69MoWKmIgCHMYCw/toan-chuc-phap-su
  val title: String, // Toàn Chức Pháp Sư
  val view: String? // 2.888.130
) {
  data class Chapter(
    val chapterLink: String, // http://www.nettruyen.com/truyen-tranh/toan-chuc-phap-su/chap-296/472537
    val chapterName: String // Chapter 296
  )
}