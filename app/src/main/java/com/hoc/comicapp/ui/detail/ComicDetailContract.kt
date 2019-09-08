package com.hoc.comicapp.ui.detail

import android.os.Parcelable
import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail
import io.reactivex.Observable
import kotlinx.android.parcel.Parcelize

interface ComicDetailInteractor {
  fun getComicDetail(
    link: String,
    name: String? = null,
    thumbnail: String? = null
  ): Observable<ComicDetailPartialChange>

  fun refreshPartialChanges(link: String): Observable<ComicDetailPartialChange>
}

sealed class ComicDetailIntent : Intent {
  data class Initial(
    val link: String,
    val thumbnail: String,
    val title: String
  ) : ComicDetailIntent()

  data class Refresh(val link: String) : ComicDetailIntent()
  data class Retry(val link: String) : ComicDetailIntent()
  data class DownloadChapter(val chapter: ComicDetailViewState.Chapter) : ComicDetailIntent()
  data class DeleteChapter(val chapter: ComicDetailViewState.Chapter) : ComicDetailIntent()
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
    data class Detail(
      val authors: List<Author>,
      val categories: List<Category>,
      val chapters: List<Chapter>,
      val lastUpdated: String,
      val link: String,
      val relatedComics: List<Comic>,
      val shortenedContent: String,
      val thumbnail: String,
      val title: String,
      val view: String
    ) : ComicDetail()

    data class Initial(
      val link: String,
      val thumbnail: String,
      val title: String
    ) : ComicDetail()
  }

  sealed class DownloadState : Parcelable {
    @Parcelize
    object Downloaded : DownloadState()

    @Parcelize
    data class Downloading(val progress: Int) : DownloadState()

    @Parcelize
    object NotYetDownload : DownloadState()

    @Parcelize
    object Loading : DownloadState()
  }

  @Parcelize
  data class Chapter(
    val chapterLink: String,
    val chapterName: String,
    val time: String,
    val view: String,
    val downloadState: DownloadState = DownloadState.Loading
  ) : Parcelable {

    fun isSameExceptDownloadState(other: Chapter): Boolean {
      if (this === other) return true
      if (chapterLink != other.chapterLink) return false
      if (chapterName != other.chapterName) return false
      if (time != other.time) return false
      if (view != other.view) return false
      return true
    }
  }

  data class Category(
    val link: String,
    val name: String
  )

  data class Author(
    val link: String,
    val name: String
  )

  data class Comic(
    val lastChapters: List<LastChapter>,
    val link: String,
    val thumbnail: String, val title: String,
    val view: String
  ) {
    data class LastChapter(
      val chapterLink: String,
      val chapterName: String,
      val time: String
    )
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

    data class InitialData(val initialComic: ComicDetail.Initial) :
      InitialRetryPartialChange()

    data class Data(val comicDetail: ComicDetail.Detail) : InitialRetryPartialChange()
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

    data class Success(val comicDetail: ComicDetail.Detail) : RefreshPartialChange()
    data class Error(val error: ComicAppError) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
  }
}

sealed class ComicDetailSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : ComicDetailSingleEvent()
}