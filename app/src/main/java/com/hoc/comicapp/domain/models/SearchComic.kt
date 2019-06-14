package com.hoc.comicapp.domain.models

data class SearchComic(
  val categoryNames: List<String>,
  val lastChapterName: String, // Chapter 14
  val link: String, // http://www.nettruyen.com/truyen-tranh/dei-ecchi-ei
  val thumbnail: String, // https://3.bp.blogspot.com/-xQZ16s48pc0/V5aqQB1OMII/AAAAAAAAFTw/eIs5Fx5BMzY/dei-ecchi-ei.jpg
  val title: String // Dei Ecchi Ei
)