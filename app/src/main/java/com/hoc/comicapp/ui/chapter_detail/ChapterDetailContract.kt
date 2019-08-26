package com.hoc.comicapp.ui.chapter_detail

import androidx.viewpager2.widget.ViewPager2
import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Companion.fromDomain
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Detail
import io.reactivex.Observable
import com.hoc.comicapp.domain.models.ChapterDetail as ChapterDetailDomain

interface ChapterDetailInteractor {
  fun getChapterDetail(
    chapterLink: String,
    chapterName: String? = null
  ): Observable<ChapterDetailPartialChange.Initial_Retry_LoadChapter_PartialChange>

  fun refresh(chapterLink: String): Observable<ChapterDetailPartialChange.RefreshPartialChange>
}

sealed class ChapterDetailViewIntent : Intent {
  data class Initial(val link: String, val name: String) : ChapterDetailViewIntent()

  object Refresh : ChapterDetailViewIntent()
  object Retry : ChapterDetailViewIntent()
  object LoadNextChapter : ChapterDetailViewIntent()
  object LoadPrevChapter : ChapterDetailViewIntent()
  data class LoadChapter(val link: String, val name: String) : ChapterDetailViewIntent()

  data class ChangeOrientation(@ViewPager2.Orientation val orientation: Int) :
    ChapterDetailViewIntent()
}

data class ChapterDetailViewState(
  val isLoading: Boolean,
  val isRefreshing: Boolean,
  val errorMessage: String?,
  val detail: Detail?,
  @ViewPager2.Orientation val orientation: Int
) : ViewState {

  data class Chapter(val name: String, val link: String) {
    override fun toString() = name
  }

  sealed class Detail {
    abstract val chapterLink: String
    abstract val chapterName: String

    data class Initial(
      override val chapterLink: String,
      override val chapterName: String
    ) : Detail()

    data class Data(
      override val chapterLink: String,
      override val chapterName: String,
      val images: List<String>,
      val chapters: List<Chapter>,
      val prevChapterLink: String?,
      val nextChapterLink: String?
    ) : Detail()
  }

  companion object {
    fun initial(): ChapterDetailViewState {
      return ChapterDetailViewState(
        isLoading = true,
        isRefreshing = false,
        detail = null,
        errorMessage = null,
        orientation = ViewPager2.ORIENTATION_VERTICAL
      )
    }

    fun fromDomain(domain: ChapterDetailDomain): Detail.Data {
      return Detail.Data(
        chapterLink = domain.chapterLink,
        chapterName = domain.chapterName,
        images = domain.images,
        chapters = domain.chapters.map {
          Chapter(
            name = it.chapterName,
            link = it.chapterLink
          )
        },
        nextChapterLink = domain.nextChapterLink,
        prevChapterLink = domain.prevChapterLink
      )
    }
  }
}


sealed class ChapterDetailPartialChange {
  abstract fun reducer(state: ChapterDetailViewState): ChapterDetailViewState

  sealed class Initial_Retry_LoadChapter_PartialChange : ChapterDetailPartialChange() {
    override fun reducer(state: ChapterDetailViewState): ChapterDetailViewState {
      return when (this) {
        is InitialData -> {
          state.copy(detail = this.initial)
        }
        is Data -> {
          state.copy(
            isLoading = false,
            errorMessage = null,
            detail = fromDomain(this.data)
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

    data class InitialData(val initial: Detail.Initial) : Initial_Retry_LoadChapter_PartialChange()
    data class Data(val data: ChapterDetailDomain) : Initial_Retry_LoadChapter_PartialChange()
    data class Error(val error: ComicAppError) : Initial_Retry_LoadChapter_PartialChange()
    object Loading : Initial_Retry_LoadChapter_PartialChange()
  }

  sealed class RefreshPartialChange : ChapterDetailPartialChange() {
    override fun reducer(state: ChapterDetailViewState): ChapterDetailViewState {
      return when (this) {
        is Success -> {
          state.copy(
            isRefreshing = false,
            errorMessage = null,
            detail = fromDomain(this.data)
          )
        }
        is Error -> {
          state.copy(
            isRefreshing = false,
            errorMessage = this.error.getMessage()
          )
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