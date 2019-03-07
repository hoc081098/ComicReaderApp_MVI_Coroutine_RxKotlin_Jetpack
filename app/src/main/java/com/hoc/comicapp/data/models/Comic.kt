package com.hoc.comicapp.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Comic(
  val chapters: List<Chapter>,
  val link: String,
  val thumbnail: String,
  val title: String,
  val view: String?
) : Parcelable