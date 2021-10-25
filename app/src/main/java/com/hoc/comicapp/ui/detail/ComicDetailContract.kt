package com.hoc.comicapp.ui.detail

import android.os.Parcelable
import com.hoc.comicapp.base.MviIntent
import com.hoc.comicapp.base.MviSingleEvent
import com.hoc.comicapp.base.MviViewState
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail
import io.reactivex.rxjava3.core.Observable
import kotlinx.parcelize.Parcelize
import java.util.Date

interface ComicDetailInteractor {
  fun getFavoriteChange(link: String): Observable<ComicDetailPartialChange>

  fun getComicDetail(
    link: String,
    name: String? = null,
    thumbnail: String? = null,
    view: String? = null,
    remoteThumbnail: String? = null,
    isDownloaded: Boolean,
  ): Observable<ComicDetailPartialChange>

  fun refreshPartialChanges(
    link: String,
    isDownloaded: Boolean,
  ): Observable<ComicDetailPartialChange>

  fun toggleFavorite(comic: ComicDetail): Observable<Unit>

  fun deleteOrCancelDownload(chapter: ComicDetailViewState.Chapter): Observable<ComicDetailSingleEvent>

  fun enqueueDownloadComic(
    chapter: ComicDetailViewState.Chapter,
    comicName: String,
    comicLink: String,
  ): Observable<ComicDetailSingleEvent>
}

sealed class ComicDetailIntent : MviIntent {
  data class Initial(val arg: Arguments.ComicDetailArgs) : ComicDetailIntent()

  object Refresh : ComicDetailIntent()
  object Retry : ComicDetailIntent()

  data class CancelDownloadChapter(val chapter: ComicDetailViewState.Chapter) : ComicDetailIntent()
  data class DownloadChapter(val chapter: ComicDetailViewState.Chapter) : ComicDetailIntent()
  data class DeleteChapter(val chapter: ComicDetailViewState.Chapter) : ComicDetailIntent()
  object ToggleFavorite : ComicDetailIntent()
}

data class ComicDetailViewState(
  val comicDetail: ComicDetail?,
  val errorMessage: String?,
  val isLoading: Boolean,
  val isRefreshing: Boolean,
  val isFavorited: Boolean?,
) : MviViewState {
  companion object {
    @JvmStatic
    fun initialState(): ComicDetailViewState = ComicDetailViewState(
      comicDetail = null,
      errorMessage = null,
      isLoading = true,
      isRefreshing = false,
      isFavorited = null
    )
  }

  sealed class ComicDetail {

    abstract val link: String
    abstract val thumbnail: String
    abstract val title: String
    abstract val view: String
    abstract val remoteThumbnail: String

    fun toDomain() = FavoriteComic(
      url = link,
      title = title,
      thumbnail = remoteThumbnail,
      view = view,
      createdAt = null
    )

    data class Detail(
      override val link: String,
      override val thumbnail: String,
      override val title: String,
      override val view: String,
      override val remoteThumbnail: String,
      val authors: List<Author>,
      val categories: List<Category>,
      val chapters: List<Chapter>,
      val lastUpdated: String,
      val relatedComics: List<Comic>,
      val shortenedContent: String,
    ) : ComicDetail()

    data class Initial(
      override val link: String,
      override val thumbnail: String,
      override val title: String,
      override val view: String,
      override val remoteThumbnail: String,
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

  data class Chapter(
    val chapterLink: String,
    val chapterName: String,
    val time: String,
    val view: String,
    val downloadState: DownloadState = DownloadState.Loading,
    val comicLink: String,
  ) {

    fun isSameExceptDownloadState(other: Chapter): Boolean {
      if (this === other) return true
      if (chapterLink != other.chapterLink) return false
      if (chapterName != other.chapterName) return false
      if (time != other.time) return false
      if (view != other.view) return false
      if (comicLink != other.comicLink) return false
      return true
    }

    fun toDownloadedChapterDomain(): DownloadedChapter {
      return DownloadedChapter(
        chapterLink = chapterLink,
        chapterName = chapterName,
        view = view,
        time = time,
        images = emptyList(),
        downloadedAt = Date(),
        comicLink = comicLink,
        prevChapterLink = null,
        nextChapterLink = null,
        chapters = emptyList()
      )
    }
  }

  data class Category(
    val link: String,
    val name: String,
  )

  data class Author(
    val link: String,
    val name: String,
  )

  data class Comic(
    val lastChapters: List<LastChapter>,
    val link: String,
    val thumbnail: String,
    val title: String,
    val view: String,
  ) {
    data class LastChapter(
      val chapterLink: String,
      val chapterName: String,
      val time: String,
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

  data class FavoriteChange(val isFavorited: Boolean?) : ComicDetailPartialChange() {
    override fun reducer(state: ComicDetailViewState) = state.copy(isFavorited = isFavorited)
  }
}

sealed class ComicDetailSingleEvent : MviSingleEvent {
  data class MessageEvent(val message: String) : ComicDetailSingleEvent()

  data class EnqueuedDownloadSuccess(val chapter: ComicDetailViewState.Chapter) :
    ComicDetailSingleEvent()

  data class EnqueuedDownloadFailure(
    val chapter: ComicDetailViewState.Chapter,
    val error: ComicAppError,
  ) : ComicDetailSingleEvent()

  data class DeletedChapter(val chapter: ComicDetailViewState.Chapter) : ComicDetailSingleEvent()

  data class DeleteChapterError(
    val chapter: ComicDetailViewState.Chapter,
    val error: ComicAppError,
  ) : ComicDetailSingleEvent()
}
