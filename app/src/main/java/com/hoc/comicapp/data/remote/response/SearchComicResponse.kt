package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class SearchComicResponse(
  @Json(name = "category_names")
  val categoryNames: List<String>,
  @Json(name = "last_chapter_name")
  val lastChapterName: String, // Chapter 14
  @Json(name = "link")
  val link: String, // http://www.nettruyen.com/truyen-tranh/dei-ecchi-ei
  @Json(name = "thumbnail")
  val thumbnail: String, // https://3.bp.blogspot.com/-xQZ16s48pc0/V5aqQB1OMII/AAAAAAAAFTw/eIs5Fx5BMzY/dei-ecchi-ei.jpg
  @Json(name = "title")
  val title: String // Dei Ecchi Ei
)