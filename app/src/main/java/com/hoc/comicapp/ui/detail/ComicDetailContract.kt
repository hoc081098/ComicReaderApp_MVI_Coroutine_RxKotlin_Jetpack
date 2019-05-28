package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.data.models.ComicAppError
import com.hoc.comicapp.data.models.getMessageFromError
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope

interface ComicDetailInteractor {
  fun getComicDetail(
    coroutineScope: CoroutineScope,
    link: String,
    name: String,
    thumbnail: String
  ): Observable<ComicDetailPartialChange>

  fun refreshPartialChanges(
    coroutineScope: CoroutineScope,
    link: String
  ): Observable<ComicDetailPartialChange>
}

sealed class ComicDetailIntent : Intent {
  data class Initial(
    val name: String,
    val link: String,
    val thumbnail: String
  ) : ComicDetailIntent()

  data class Refresh(val link: String) : ComicDetailIntent()
}

data class Category(val name: String, val link: String)

data class Chapter(
  val name: String,
  val link: String,
  val time: String?,
  val view: String?
)

sealed class ComicDetail {
  abstract val link: String
  abstract val thumbnail: String
  abstract val title: String

  data class Comic(
    override val link: String,
    override val thumbnail: String,
    override val title: String,
    val view: String,
    val lastUpdated: String,
    val author: String,
    val status: String,
    val categories: List<Category>,
    val otherName: String?,
    val shortenedContent: String,
    val chapters: List<Chapter>
  ) : ComicDetail()

  data class InitialComic(
    override val title: String,
    override val thumbnail: String,
    override val link: String
  ) : ComicDetail()
}

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
          state.copy(comicDetail = this.initialComic)
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
            errorMessage = this.error.getMessageFromError()
          )
        }
        Loading -> {
          state.copy(isLoading = true)
        }
      }
    }

    data class InitialData(val initialComic: ComicDetail.InitialComic) : InitialPartialChange()
    data class Data(val comicDetail: ComicDetail.Comic) : InitialPartialChange()
    data class Error(val error: ComicAppError) : InitialPartialChange()
    object Loading : InitialPartialChange()
  }

  sealed class RefreshPartialChange : ComicDetailPartialChange() {
    override fun reducer(state: ComicDetailViewState): ComicDetailViewState {
      return when (this) {
        is Success -> {
          state.copy(
            isLoading = false,
            errorMessage = null,
            comicDetail = this.comicDetail
          )
        }
        is Error -> {
          state.copy(isLoading = false)
        }
        Loading -> {
          state.copy(isLoading = true)
        }
      }
    }

    data class Success(val comicDetail: ComicDetail.Comic) : RefreshPartialChange()
    data class Error(val error: ComicAppError) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
  }
}

sealed class ComicDetailSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : ComicDetailSingleEvent()
}