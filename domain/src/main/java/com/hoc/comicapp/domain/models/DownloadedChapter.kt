package com.hoc.comicapp.domain.models

import java.util.Date

data class DownloadedChapter(
  val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-0-275968490470920

  val chapterName: String, // Chapter 0

  val time: String, // December 2018

  val view: String, // 9592

  val images: List<String>, // []

  val comicLink: String,

  val downloadedAt: Date,

  val chapters: List<DownloadedChapter>,

  val prevChapterLink: String?, // https://ww2.mangafox.online/solo-leveling/chapter-64-1137969534934196

  val nextChapterLink: String?,
)
