package com.hoc.comicapp.ui.chapter_detail

import androidx.viewpager2.widget.ViewPager2
import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.InitialRetryLoadChapterPartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.RefreshPartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Chapter
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Detail
import io.reactivex.Observable
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Detail.Initial as InitialVS

interface ChapterDetailInteractor {
  fun getChapterDetail(chapter: Chapter): Observable<InitialRetryLoadChapterPartialChange>

  fun refresh(chapter: Chapter): Observable<RefreshPartialChange>
}

sealed class ChapterDetailViewIntent : Intent {
  data class Initial(val chapter: Chapter) : ChapterDetailViewIntent()
  object Refresh : ChapterDetailViewIntent()
  object Retry : ChapterDetailViewIntent()
  object LoadNextChapter : ChapterDetailViewIntent()
  object LoadPrevChapter : ChapterDetailViewIntent()
  data class LoadChapter(val chapter: Chapter) : ChapterDetailViewIntent()
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
    abstract val chapter: Chapter

    data class Initial(override val chapter: Chapter) : Detail()

    data class Data(
      override val chapter: Chapter,
      val images: List<String>,
      val chapters: List<Chapter>,
      val prevChapterLink: String?,
      val nextChapterLink: String?
    ) : Detail()

    companion object {
      @JvmStatic
      fun fromDomain(domain: com.hoc.comicapp.domain.models.ChapterDetail): Data {
        return Data(
          chapter = Chapter(
            name = domain.chapterName,
            link = domain.chapterLink
          ),
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

  companion object {
    @JvmStatic
    fun initial(): ChapterDetailViewState {
      return ChapterDetailViewState(
        isLoading = true,
        isRefreshing = false,
        detail = null,
        errorMessage = null,
        orientation = ViewPager2.ORIENTATION_VERTICAL
      )
    }
  }
}

sealed class ChapterDetailPartialChange {
  abstract fun reducer(state: ChapterDetailViewState): ChapterDetailViewState

  sealed class InitialRetryLoadChapterPartialChange : ChapterDetailPartialChange() {
    override fun reducer(state: ChapterDetailViewState): ChapterDetailViewState {
      return when (this) {
        is InitialData -> {
          state.copy(detail = this.initial)
        }
        is Data -> {
          state.copy(
            isLoading = false,
            errorMessage = null,
            detail = this.data
          )
        }
        is Error -> {
          state.copy(
            isLoading = false,
            errorMessage = this.error.getMessage(),
            detail = this.data
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

    data class InitialData(val initial: InitialVS) : InitialRetryLoadChapterPartialChange()
    data class Data(val data: Detail.Data) : InitialRetryLoadChapterPartialChange()
    data class Error(val error: ComicAppError, val data: InitialVS) :
      InitialRetryLoadChapterPartialChange()

    object Loading : InitialRetryLoadChapterPartialChange()
  }

  sealed class RefreshPartialChange : ChapterDetailPartialChange() {
    override fun reducer(state: ChapterDetailViewState): ChapterDetailViewState {
      return when (this) {
        is Success -> {
          state.copy(
            isRefreshing = false,
            errorMessage = null,
            detail = this.data
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

    data class Success(val data: Detail.Data) : RefreshPartialChange()
    data class Error(val error: ComicAppError) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
  }
}

sealed class ChapterDetailSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : ChapterDetailSingleEvent()
}