package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item.ComicItem
import io.reactivex.Observable

interface SearchComicContract {
  interface Interactor {
    fun searchComic(term: String): Observable<PartialChange>
  }

  data class ViewState(
    val isLoading: Boolean,
    val comics: List<Item>,
    val errorMessage: String?
  ) : com.hoc.comicapp.base.ViewState {
    companion object {
      @JvmStatic
      fun initialState() = ViewState(
        isLoading = false,
        comics = emptyList(),
        errorMessage = null
      )
    }

    sealed class Item {
      data class ComicItem(
        val title: String,
        val thumbnail: String,
        val link: String,
        val lastChapters: List<ChapterItem>
      ) : Item() {
        constructor(domain: Comic) : this(
          title = domain.title,
          thumbnail = domain.thumbnail,
          link = domain.link,
          lastChapters = domain.lastChapters.map(::ChapterItem)
        )
      }
    }

    data class ChapterItem(
      val chapterName: String,
      val time: String
    ) {
      constructor(domain: Comic.LastChapter) : this(
        time = domain.time,
        chapterName = domain.chapterName
      )
    }
  }

  sealed class PartialChange {
    fun reducer(state: ViewState): ViewState {
      return when (this) {
        is Data -> {
          state.copy(
            isLoading = false,
            errorMessage = null,
            comics = comics
          )
        }
        Loading -> {
          state.copy(
            isLoading = true,
            errorMessage = null,
            comics = emptyList()
          )
        }
        is Error -> {
          state.copy(
            isLoading = false,
            errorMessage = "Search for '$term', error occurred: ${error.getMessage()}",
            comics = emptyList()
          )
        }
      }
    }

    data class Data(val comics: List<ComicItem>) : PartialChange()
    object Loading : PartialChange()
    data class Error(val error: ComicAppError, val term: String) : PartialChange()
  }

  sealed class ViewIntent : Intent {
    data class SearchIntent(val term: String) : ViewIntent()
    object RetryIntent : ViewIntent()
  }

  sealed class SingleEvent : com.hoc.comicapp.base.SingleEvent {
    data class MessageEvent(val message: String) : SingleEvent()
  }
}

