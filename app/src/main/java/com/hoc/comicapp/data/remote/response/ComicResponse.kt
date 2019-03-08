package com.hoc.comicapp.data.remote.response

import com.squareup.moshi.Json

data class ComicResponse(
  @Json(name = "chapters") val chapters: List<ChapterResponse>,
  @Json(name = "link") val link: String,
  @Json(name = "thumbnail") val thumbnail: String,
  @Json(name = "title") val title: String,
  @Json(name = "view") val view: String? = null,
  @Json(name = "more_detail") val moreDetail: MoreDetailResponse? = null
)

data class MoreDetailResponse(
  @Json(name = "last_updated") val lastUpdated: String,
  @Json(name = "author") val author: String,
  @Json(name = "status") val status: String,
  @Json(name = "categories") val categories: List<CategoryResponse>,
  @Json(name = "othername") val otherName: String? = null,
  @Json(name = "shortened_content") val shortenedContent: String
)