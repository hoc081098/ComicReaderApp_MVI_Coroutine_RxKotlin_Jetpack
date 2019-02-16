package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class ComicResponse(
  @Json(name = "chapters") val chapters: List<ChapterResponse>,
  @Json(name = "link") val link: String,
  @Json(name = "thumbnail") val thumbnail: String,
  @Json(name = "title") val title: String,
  @Json(name = "view") val view: String? = null
)