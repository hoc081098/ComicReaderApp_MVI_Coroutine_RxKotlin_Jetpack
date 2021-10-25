package com.hoc.comicapp.domain.models

data class Comic(
  val lastChapters: List<LastChapter>,
  val link: String, // https://ww2.mangafox.online/volcanic-age
  val thumbnail: String, // https://cdn1.mangafox.online/132/857/695/330/341/5/volcanic-age.jpg
  val title: String, // Volcanic Age
  val view: String, // 1.1k
) {
  data class LastChapter(
    val chapterLink: String, // https://ww2.mangafox.online/volcanic-age/chapter-90-512420270425665
    val chapterName: String, // Chapter 90
    val time: String, // 2 days ago
  )
}
