package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class ErrorResponse(
  @Json(name = "message") val message: String,
  @Json(name = "statusCode") val statusCode: Int
)