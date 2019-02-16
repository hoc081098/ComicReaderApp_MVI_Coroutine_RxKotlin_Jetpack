package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class ChapterResponse(
  @Json(name = "chapter_link") val chapterLink: String,
  @Json(name = "chapter_name") val chapterName: String,
  @Json(name = "time") val time: String? = null
)