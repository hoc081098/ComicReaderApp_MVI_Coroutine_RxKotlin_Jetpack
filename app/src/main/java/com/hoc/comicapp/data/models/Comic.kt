package com.hoc.comicapp.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Comic(
  val chapters: List<Chapter>,
  val link: String,
  val thumbnail: String,
  val title: String,
  val view: String?,
  val moreDetail: MoreDetail?
) : Parcelable

@Parcelize
data class MoreDetail(
  val lastUpdated: String,
  val author: String,
  val status: String,
  val categories: List<Category>,
  val otherName: String?,
  val shortenedContent: String
) : Parcelable