package com.hoc.comicapp.ui.downloaded_comics

import android.app.Application
import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import java.util.*
import com.hoc.comicapp.base.SingleEvent as BaseSingleEvent
import com.hoc.comicapp.base.ViewState as BaseViewState

interface DownloadedComicsContract {
  interface Interactor {
    fun getDownloadedComics(): Observable<PartialChange>
    fun deleteComic(comicItem: ComicItem): Single<Pair<ComicItem, ComicAppError?>>
  }

  sealed class ViewIntent : Intent {
    object Initial : ViewIntent()
    data class ChangeSortOrder(val order: SortOrder) : ViewIntent()
    data class DeleteComic(val comic: ComicItem) : ViewIntent()
  }

  enum class SortOrder(val description: String) {
    ComicTitleAsc("Comic title ascending"),
    ComicTitleDesc("Comic title descending"),
    LatestChapterAsc("Latest chapter first"),
    LatestChapterDesc("Latest chapter last");

    override fun toString() = description
  }

  data class ViewState(
    val isLoading: Boolean,
    val error: String?,
    val comics: List<ComicItem>
  ) : BaseViewState {

    companion object {
      fun initial(): ViewState {
        return ViewState(
          isLoading = true,
          error = null,
          comics = emptyList()
        )
      }
    }

    data class ComicItem(
      val title: String,
      val comicLink: String,
      val thumbnail: File,
      val view: String,
      val chapters: List<ChapterItem>
    ) {
      fun toDomain(): DownloadedComic {
        return DownloadedComic(
          comicLink = comicLink,
          view = view,
          title = title,
          chapters = chapters.map {
            DownloadedChapter(
              comicLink = it.link,
              chapterName = it.chapterName,
              downloadedAt = it.downloadedAt,
              view = "",
              images = emptyList(),
              time = "",
              chapterLink = ""
            )
          },
          shortenedContent = "",
          lastUpdated = "",
          thumbnail = "",
          authors = emptyList(),
          categories = emptyList()
        )
      }

      companion object {
        @JvmStatic
        fun fromDomain(comic: DownloadedComic, application: Application): ComicItem {
          return ComicItem(
            title = comic.title,
            comicLink = comic.comicLink,
            view = comic.view,
            thumbnail = File(application.filesDir, comic.thumbnail),
            chapters = comic.chapters.map {
              ChapterItem(
                chapterName = it.chapterName,
                downloadedAt = it.downloadedAt,
                link = it.chapterLink
              )
            }
          )
        }
      }
    }

    data class ChapterItem(
      val link: String,
      val chapterName: String,
      val downloadedAt: Date
    )
  }

  sealed class PartialChange {
    data class Data(val comics: List<ComicItem>) : PartialChange()

    data class Error(val error: ComicAppError) : PartialChange()

    object Loading : PartialChange()
  }

  sealed class SingleEvent : BaseSingleEvent {
    data class Message(val message: String) : SingleEvent()

    data class DeletedComic(val comic: ComicItem) : SingleEvent()

    data class DeleteComicError(val comic: ComicItem, val error: ComicAppError) : SingleEvent()
  }
}