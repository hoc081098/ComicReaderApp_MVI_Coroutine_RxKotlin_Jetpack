package com.hoc.comicapp.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Chapter(
  val chapterLink: String,
  val chapterName: String,
  val time: String?,
  val images: List<String>?,
  val view: String?
) : Parcelable