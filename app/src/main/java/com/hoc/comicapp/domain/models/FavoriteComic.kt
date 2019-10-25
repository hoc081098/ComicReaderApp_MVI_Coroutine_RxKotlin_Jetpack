package com.hoc.comicapp.domain.models

import java.util.*

data class FavoriteComic(
  val title: String,
  val thumbnail: String,
  val createdAt: Date
)