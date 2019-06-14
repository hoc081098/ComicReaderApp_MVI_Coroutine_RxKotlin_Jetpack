package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.domain.models.ComicAppError
import com.hoc.domain.models.getMessage
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import com.hoc.domain.models.ComicDetail as ComicDetailDomain

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
    val link: String,
    val thumbnail: String,
    val title: String
  ) : ComicDetailIntent()

  data class Refresh(val link: String) : ComicDetailIntent()
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
            errorMessage = this.error.getMessage()
          )
        }
        Loading -> {
          state.copy(isLoading = true)
        }
      }
    }

    data class InitialData(val initialComic: ComicDetailViewState.ComicDetail.InitialComic) : InitialPartialChange()
    data class Data(val comicDetail: ComicDetailViewState.ComicDetail.Comic) : InitialPartialChange()
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

    data class Success(val comicDetail: ComicDetailViewState.ComicDetail.Comic) : RefreshPartialChange()
    data class Error(val error: ComicAppError) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
  }
}

sealed class ComicDetailSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : ComicDetailSingleEvent()
}