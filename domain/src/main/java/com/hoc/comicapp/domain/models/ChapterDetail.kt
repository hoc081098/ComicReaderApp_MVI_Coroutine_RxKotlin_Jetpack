package com.hoc.comicapp.domain.models

data class ChapterDetail(
  val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-65-159349729567655
  val chapterName: String, // Chapter 65
  val chapters: List<Chapter>,
  val images: List<String>,
  val prevChapterLink: String?, // https://ww2.mangafox.online/solo-leveling/chapter-64-1137969534934196
  val nextChapterLink: String?, // https://ww2.mangafox.online/solo-leveling/chapter-64-1137969534934196
) {
  data class Chapter(
    val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-0-275968490470920
    val chapterName: String, // Chapter 0
  )
}
