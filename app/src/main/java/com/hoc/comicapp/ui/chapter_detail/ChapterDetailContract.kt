package com.hoc.comicapp.ui.chapter_detail

import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.getMessage
import io.reactivex.rxjava3.core.Observable
import timber.log.Timber

interface ChapterDetailContract {

  interface Interactor {
    fun getChapterDetail(
      chapter: ViewState.Chapter,
      isDownloaded: Boolean,
    ): Observable<PartialChange>

    fun refresh(chapter: ViewState.Chapter, isDownloaded: Boolean): Observable<PartialChange>
  }

  sealed class ViewIntent : com.hoc.comicapp.base.MviIntent {
    data class Initial(val chapter: ViewState.Chapter) : ViewIntent()
    object Refresh : ViewIntent()
    object Retry : ViewIntent()
    object LoadNextChapter : ViewIntent()
    object LoadPrevChapter : ViewIntent()
    data class LoadChapter(val chapter: ViewState.Chapter) : ViewIntent()
    data class ChangeOrientation(@RecyclerView.Orientation val orientation: Int) :
      ViewIntent()
  }

  data class ViewState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val errorMessage: String?,
    val detail: Detail?,
    @RecyclerView.Orientation val orientation: Int,
  ) : com.hoc.comicapp.base.MviViewState {

    data class Chapter(val name: String, val link: String) {
      override fun toString() = name
      val debug get() = "Chapter { name=$name, link=$link }"
    }

    sealed class Detail {
      abstract val chapter: Chapter
      abstract val chapters: List<Chapter>

      data class Initial(
        override val chapter: Chapter,
        override val chapters: List<Chapter> = listOf(chapter),
      ) : Detail()

      data class Data(
        override val chapter: Chapter,
        val images: List<String>,
        val prevChapterLink: String?,
        val nextChapterLink: String?,
        override val chapters: List<Chapter>,
      ) : Detail()

      companion object {
        @JvmStatic fun fromDomain(domain: com.hoc.comicapp.domain.models.ChapterDetail): Data {
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

        @JvmStatic fun fromDomain(domain: DownloadedChapter): Data {
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
      fun initial(): ViewState {
        return ViewState(
          isLoading = true,
          isRefreshing = false,
          detail = null,
          errorMessage = null,
          orientation = RecyclerView.VERTICAL
        )
      }
    }

    fun withChapter(chapter: Chapter): Detail {
      val detail = detail
        ?: return Detail.Initial(chapter = chapter)

      // copy new state with previous chapters list

      val chapters = detail.chapters
      val index = chapters.indexOfFirst { it.link == chapter.link }
      Timber.tag("LoadChapter###").d("::reducer $index $detail")

      return Detail.Data(
        chapter = chapter,
        chapters = chapters,
        images = emptyList(),
        nextChapterLink = chapters.getOrNull(index + 1)?.link,
        prevChapterLink = chapters.getOrNull(index - 1)?.link
      )
    }
  }

  sealed class PartialChange {
    abstract fun reducer(state: ViewState): ViewState

    sealed class GetChapterDetail : PartialChange() {
      override fun reducer(state: ViewState): ViewState {
        return when (this) {
          is Initial -> {
            state.copy(detail = state.withChapter(chapter))
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
              detail = state.withChapter(chapter)
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

      data class Initial(val chapter: ViewState.Chapter) : GetChapterDetail()

      data class Data(val data: ViewState.Detail.Data) : GetChapterDetail()
      data class Error(
        val error: ComicAppError,
        val chapter: ViewState.Chapter,
      ) : GetChapterDetail()

      object Loading : GetChapterDetail()
    }

    sealed class Refresh : PartialChange() {
      override fun reducer(state: ViewState): ViewState {
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

      data class Success(val data: ViewState.Detail.Data) : Refresh()
      data class Error(val error: ComicAppError) : Refresh()
      object Loading : Refresh()
    }
  }

  sealed class SingleEvent : com.hoc.comicapp.base.MviSingleEvent {
    data class MessageEvent(val message: String) : SingleEvent()
  }
}
