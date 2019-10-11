package com.hoc.comicapp.ui.category_detail

import android.os.Parcelable
import com.hoc.comicapp.domain.models.ComicAppError
import kotlinx.android.parcel.Parcelize

interface CategoryDetailContract {

  @Parcelize
  data class CategoryArg(
    val description: String,
    val link: String,
    val name: String,
    val thumbnail: String
  ) : Parcelable

  sealed class ViewIntent : com.hoc.comicapp.base.Intent {

  }

  data class ViewState(
    val items: List<Item>,
    val isRefreshing: Boolean
  ) : com.hoc.comicapp.base.ViewState {
    companion object {
      @JvmStatic
      fun initial(): ViewState {
        return ViewState(
          items = emptyList(),
          isRefreshing = false
        )
      }
    }

    sealed class Item {
      data class Popular(
        val comics: List<PopularItem>,
        val error: ComicAppError,
        val isLoading: Boolean
      ) : Item()

      //TODO
      data class Comic(val TODO: Any) : Item()

      object Loading : Item()

      data class Error(val error: ComicAppError) : Item()
    }

    //TODO
    data class PopularItem(val TODO: Any)
  }

  sealed class SingleEvent : com.hoc.comicapp.base.SingleEvent {

  }
}