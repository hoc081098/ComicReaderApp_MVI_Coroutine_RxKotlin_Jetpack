package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class CategoryResponse(
  @Json(name = "name") val name: String,
  @Json(name = "link") val link: String,
  @Json(name = "desciption") val description: String? = null
)