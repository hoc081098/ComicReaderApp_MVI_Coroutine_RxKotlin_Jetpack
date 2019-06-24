package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import com.hoc.comicapp.domain.models.ComicDetail as ComicDetailDomain

interface ComicDetailInteractor {
  fun getComicDetail(
    coroutineScope: CoroutineScope,
    link: String,
    name: String? = null,
    thumbnail: String? = null
  ): Observable<ComicDetailPartialChange>

  fun refreshPartialChanges(
    coroutineScope: CoroutineScope,
    link: String
  ): Observable<ComicDetailPartialChange>
}

sealed class ComicDetailIntent : Intent {
  data class Initial(
    val link: String,
    val thumbnail: String,
    val title: String
  ) : ComicDetailIntent()

  data class Refresh(val link: String) : ComicDetailIntent()
  data class Retry(val link: String) : ComicDetailIntent()
}

data class ComicDetailViewState(
  val comicDetail: ComicDetail?,
  val errorMessage: String?,
  val isLoading: Boolean,
  val isRefreshing: Boolean
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState(): ComicDetailViewState = ComicDetailViewState(
      comicDetail = null,
      errorMessage = null,
      isLoading = true,
      isRefreshing = false
    )
  }

  sealed class ComicDetail {
    data class Comic(val comicDetail: ComicDetailDomain) : ComicDetail()

    data class InitialComic(
      val link: String,
      val thumbnail: String,
      val title: String
    ) : ComicDetail()
  }
}

sealed class ComicDetailPartialChange {
  abstract fun reducer(state: ComicDetailViewState): ComicDetailViewState

  sealed class InitialRetryPartialChange : ComicDetailPartialChange() {
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
            errorMessage = this.error.getMessage()
          )
        }
        Loading -> {
          state.copy(
            isLoading = true,
            errorMessage = null
          )
        }
      }
    }

    data class InitialData(val initialComic: ComicDetailViewState.ComicDetail.InitialComic) :
      InitialRetryPartialChange()

    data class Data(val comicDetail: ComicDetailViewState.ComicDetail.Comic) : InitialRetryPartialChange()
    data class Error(val error: ComicAppError) : InitialRetryPartialChange()
    object Loading : InitialRetryPartialChange()
  }

  sealed class RefreshPartialChange : ComicDetailPartialChange() {
    override fun reducer(state: ComicDetailViewState): ComicDetailViewState {
      return when (this) {
        is Success -> {
          state.copy(
            isRefreshing = false,
            errorMessage = null,
            comicDetail = this.comicDetail
          )
        }
        is Error -> {
          state.copy(isRefreshing = false)
        }
        Loading -> {
          state.copy(isRefreshing = true)
        }
      }
    }

    data class Success(val comicDetail: ComicDetailViewState.ComicDetail.Comic) : RefreshPartialChange()
    data class Error(val error: ComicAppError) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
  }
}

sealed class ComicDetailSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : ComicDetailSingleEvent()
}