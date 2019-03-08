package com.hoc.comicapp.data.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class Category(
  val name: String,
  val link: String
) : Parcelable