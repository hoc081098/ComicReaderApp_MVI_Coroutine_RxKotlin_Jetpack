package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Detail
import io.reactivex.Observable
import com.hoc.comicapp.domain.models.ChapterDetail as ChapterDetailDomain

interface ChapterDetailInteractor {
  fun getChapterDetail(
    chapterLink: String,
    chapterName: String? = null,
    time: String? = null,
    view: String? = null
  ): Observable<ChapterDetailPartialChange.InitialRetryPartialChange>

  fun refresh(chapterLink: String): Observable<ChapterDetailPartialChange.RefreshPartialChange>
}

sealed class ChapterDetailViewIntent : Intent {
  data class Initial(val initial: Detail.Initial) :
    ChapterDetailViewIntent()

  data class Refresh(val link: String)
  data class Retry(val link: String)
}

data class ChapterDetailViewState(
  val isLoading: Boolean,
  val isRefreshing: Boolean,
  val errorMessage: String?,
  val detail: Detail?
) : ViewState {

  sealed class Detail {
    data class Initial(
      val chapterLink: String,
      val chapterName: String,
      val time: String,
      val view: String
    ) : Detail()

    data class Data(val chapterDetail: ChapterDetailDomain) : Detail()
  }

  companion object {
    fun initial(): ChapterDetailViewState {
      return ChapterDetailViewState(
        isLoading = true,
        isRefreshing = false,
        detail = null,
        errorMessage = null
      )
    }
  }
}

sealed class ChapterDetailPartialChange {
  abstract fun reducer(state: ChapterDetailViewState): ChapterDetailViewState

  sealed class InitialRetryPartialChange : ChapterDetailPartialChange() {
    override fun reducer(state: ChapterDetailViewState): ChapterDetailViewState {
      return when (this) {
        is InitialData -> {
          state.copy(detail = this.initial)
        }
        is Data -> {
          state.copy(
            isLoading = false,
            errorMessage = null,
            detail = Detail.Data(this.data)
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

    data class InitialData(val initial: Detail.Initial) : InitialRetryPartialChange()
    data class Data(val data: ChapterDetailDomain) : InitialRetryPartialChange()
    data class Error(val error: ComicAppError) : InitialRetryPartialChange()
    object Loading : InitialRetryPartialChange()
  }

  sealed class RefreshPartialChange : ChapterDetailPartialChange() {
    override fun reducer(state: ChapterDetailViewState): ChapterDetailViewState {
      return when (this) {
        is Success -> {
          state.copy(
            isRefreshing = false,
            errorMessage = null,
            detail = Detail.Data(this.data)
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

    data class Success(val data: ChapterDetailDomain) : RefreshPartialChange()
    data class Error(val error: ComicAppError) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
  }
}

sealed class ChapterDetailSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : ChapterDetailSingleEvent()
}