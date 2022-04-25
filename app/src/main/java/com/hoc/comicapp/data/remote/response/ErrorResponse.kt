package com.hoc.comicapp.data.remote.response

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class ErrorResponse(
  @Json(name = "message") val message: String,
  @Json(name = "status_code") val statusCode: Int,
)
