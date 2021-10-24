package com.hoc.comicapp.data.remote.response

import androidx.annotation.Keep
import com.squareup.moshi.Json

@Keep
data class ErrorResponse(
  @field:Json(name = "message") val message: String,
  @field:Json(name = "status_code") val statusCode: Int,
)
