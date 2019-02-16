package com.hoc.comicapp.data.models

data class Comic(
  val chapters: List<Chapter>,
  val link: String,
  val thumbnail: String,
  val title: String,
  val view: String?
)