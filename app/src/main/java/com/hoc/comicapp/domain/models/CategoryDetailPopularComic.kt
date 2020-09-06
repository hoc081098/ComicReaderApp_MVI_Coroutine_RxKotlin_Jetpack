package com.hoc.comicapp.domain.models

data class CategoryDetailPopularComic(
  val lastChapter: LastChapter,
  val link: String, // https://ww2.mangafox.online/the-fiancee-is-here
  val thumbnail: String, // https://cdn1.mangafox.online/625/080/369/821/818/the-fiancee-is-here.jpg
  val title: String, // The Fiancee is Here
) {
  data class LastChapter(
    val chapterLink: String, // https://ww2.mangafox.online/the-fiancee-is-here/chapter-13-648903251249287
    val chapterName: String, // Chapter 13
  )
}
