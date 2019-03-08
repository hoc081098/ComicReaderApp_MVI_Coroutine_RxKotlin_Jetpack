package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.data.models.ComicAppError
import com.hoc.comicapp.data.models.getMessageFromError
import io.reactivex.ObservableSource
import kotlinx.coroutines.CoroutineScope

interface ComicDetailInteractor {
  fun getComicDetail(
    coroutineScope: CoroutineScope,
    link: String,
    name: String,
    thumbnail: String
  ): ObservableSource<ComicDetailPartialChange>

}

sealed class ComicDetailIntent : Intent {
  data class Initial(
    val name: String,
    val link: String,
    val thumbnail: String
  ) : ComicDetailIntent()

  object Refresh : ComicDetailIntent()
}

data class Category(val name: String, val link: String)

data class ComicDetail(
  val link: String,
  val thumbnail: String,
  val title: String,
  val view: String,
  val lastUpdated: String,
  val author: String,
  val status: String,
  val categories: List<Category>,
  val otherName: String?,
  val shortenedContent: String
)

data class ComicDetailViewState(
  val comicDetail: ComicDetail?,
  val errorMessage: String?,
  val isLoading: Boolean
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState(): ComicDetailViewState = ComicDetailViewState(
      comicDetail = null,
      errorMessage = null,
      isLoading = true
    )
  }
}

sealed class ComicDetailPartialChange {
  abstract fun reducer(state: ComicDetailViewState): ComicDetailViewState

  sealed class InitialPartialChange : ComicDetailPartialChange() {
    override fun reducer(state: ComicDetailViewState): ComicDetailViewState {
      return when (this) {
        is InitialData -> {
          state.copy(
            comicDetail = state.comicDetail?.copy(
              title = this.name,
              thumbnail = this.thumbnail
            )
          )
        }
        is Data -> {
          state.copy(
            isLoading = false,
            errorMessage = null,
            comicDetail = this.comicDetail
          )
        }
        is Error -> {
          state.copy(
            isLoading = false,
            errorMessage = getMessageFromError(this.error)
          )
        }
        Loading -> {
          state.copy(isLoading = true)
        }
      }
    }

    data class InitialData(val name: String, val thumbnail: String) : InitialPartialChange()
    data class Data(val comicDetail: ComicDetail) : InitialPartialChange()
    data class Error(val error: ComicAppError) : InitialPartialChange()
    object Loading : InitialPartialChange()
  }
}

sealed class ComicDetailSingleEvent : SingleEvent {

}