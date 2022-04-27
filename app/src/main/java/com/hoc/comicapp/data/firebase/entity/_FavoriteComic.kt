package com.hoc.comicapp.data.firebase.entity

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import com.hoc.comicapp.domain.models.FavoriteComic

@Suppress("ClassName")
@IgnoreExtraProperties
data class _FavoriteComic(
  @get:PropertyName("url") @set:PropertyName("url")
  var url: String,
  @get:PropertyName("title") @set:PropertyName("title")
  var title: String,
  @get:PropertyName("thumbnail") @set:PropertyName("thumbnail")
  var thumbnail: String,
  @get:PropertyName("view") @set:PropertyName("view")
  var view: String,
  @get:ServerTimestamp @get:PropertyName("created_at") @set:PropertyName("created_at")
  var createdAt: Timestamp?,
) {
  fun toDomain(): FavoriteComic {
    return FavoriteComic(
      title = title,
      thumbnail = thumbnail,
      createdAt = createdAt?.toDate(),
      url = url,
      view = view
    )
  }

  fun asMap() = mapOf(
    "view" to view,
    "thumbnail" to thumbnail,
    "title" to title
  )

  @Suppress("unused")
  constructor() : this(
    url = "",
    createdAt = null,
    thumbnail = "",
    title = "",
    view = ""
  )
}
