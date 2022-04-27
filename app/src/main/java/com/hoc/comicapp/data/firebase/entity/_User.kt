package com.hoc.comicapp.data.firebase.entity

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.hoc.comicapp.domain.models.User

@Suppress("ClassName")
@IgnoreExtraProperties
data class _User(
  @get:PropertyName("uid") @set:PropertyName("uid")
  var uid: String,
  @get:PropertyName("display_name") @set:PropertyName("display_name")
  var displayName: String,
  @get:PropertyName("email") @set:PropertyName("email")
  var email: String,
  @get:PropertyName("photo_url") @set:PropertyName("photo_url")
  var photoURL: String,
) {
  @Suppress("unused")
  constructor() : this("", "", "", "")

  fun toDomain() = User(
    uid = uid,
    displayName = displayName,
    photoURL = photoURL,
    email = email
  )
}
