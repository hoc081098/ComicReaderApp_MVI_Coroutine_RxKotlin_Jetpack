// ktlint-disable filename

package com.hoc.comicapp.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object Arguments {
  /**
   * Argument to pass to [com.hoc.comicapp.ui.detail.ComicDetailFragment]
   */
  @Parcelize
  data class ComicDetailArgs(
    val link: String,
    val thumbnail: String,
    val title: String,
    val view: String,
    val remoteThumbnail: String,
  ) : Parcelable

  /**
   * Argument to pass to [com.hoc.comicapp.ui.category_detail.CategoryDetailFragment]
   */
  @Parcelize
  data class CategoryDetailArgs(
    val description: String,
    val link: String,
    val name: String,
    val thumbnail: String,
  ) : Parcelable

  /**
   * Argument to pass to [com.hoc.comicapp.ui.chapter_detail.ChapterDetailFragment]
   */
  @Parcelize
  data class ChapterDetailArgs(
    val chapterLink: String,
    val chapterName: String,
    val time: String,
    val view: String,
    val comicLink: String,
  ) : Parcelable
}
