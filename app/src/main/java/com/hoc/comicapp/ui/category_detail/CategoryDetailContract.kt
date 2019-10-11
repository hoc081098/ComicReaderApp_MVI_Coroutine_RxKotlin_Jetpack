package com.hoc.comicapp.ui.category_detail

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

interface CategoryDetailContract {

  @Parcelize
  data class CategoryArg(
    val description: String,
    val link: String,
    val name: String,
    val thumbnail: String
  ) : Parcelable
}