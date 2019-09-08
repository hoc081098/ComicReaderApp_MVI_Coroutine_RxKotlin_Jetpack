package com.hoc.comicapp.domain.models

import java.util.*


data class DownloadedChapter(
  val chapterLink: String, // https://ww2.mangafox.online/solo-leveling/chapter-0-275968490470920

  val chapterName: String, // Chapter 0

  val time: String, // December 2018

  val view: String, // 9592

  val images: List<String>, // []

  val comicLink: String,

  val downloadedAt: Date
)