package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.base.MviIntent
import com.hoc.comicapp.base.MviSingleEvent
import com.hoc.comicapp.base.MviViewState
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item.ComicItem
import io.reactivex.rxjava3.core.Observable

interface SearchComicContract {
  interface Interactor {
    fun searchComic(term: String, page: Int): Observable<PartialChange>
  }

  data class ViewState(
    val isLoading: Boolean,
    val comics: List<Item>,
    val errorMessage: String?,
    val page: Int,
  ) : MviViewState {
    companion object {
      @JvmStatic
      fun initialState() = ViewState(
        isLoading = false,
        comics = emptyList(),
        errorMessage = null,
        page = 0
      )
    }

    sealed class Item {
      data class ComicItem(
        val title: String,
        val thumbnail: String,
        val link: String,
        val lastChapters: List<ChapterItem>,
        val view: String,
      ) : Item() {
        constructor(domain: Comic) : this(
          title = domain.title,
          thumbnail = domain.thumbnail,
          link = domain.link,
          lastChapters = domain.lastChapters.map(::ChapterItem),
          view = domain.view
        )
      }

      object Idle : Item()
      object Loading : Item()
      data class Error(val errorMessage: String) : Item()
    }

    data class ChapterItem(
      val chapterName: String,
      val time: String,
    ) {
      constructor(domain: Comic.LastChapter) : this(
        time = domain.time,
        chapterName = domain.chapterName
      )
    }
  }

  sealed class PartialChange {
    abstract fun reducer(state: ViewState): ViewState

    sealed class FirstPage : PartialChange() {
      data class Data(val comics: List<ComicItem>) : FirstPage()
      object Loading : FirstPage()
      data class Error(val error: ComicAppError, val term: String) : FirstPage()

      override fun reducer(state: ViewState): ViewState {
        return when (this) {
          is Data -> {
            state.copy(
              isLoading = false,
              errorMessage = null,
              comics = comics + if (comics.isNotEmpty()) listOf(Item.Idle) else emptyList(),
              page = 1
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
    }

    sealed class NextPage : PartialChange() {
      data class Data(val comics: List<ComicItem>) : NextPage()
      object Loading : NextPage()
      data class Error(val error: ComicAppError, val term: String) : NextPage()

      override fun reducer(state: ViewState): ViewState {
        val oldComics = state.comics.filterIsInstance<ComicItem>()

        return when (this) {
          is Data -> {
            state.copy(
              isLoading = false,
              errorMessage = null,
              comics = oldComics + comics + if (comics.isNotEmpty()) {
                listOf(Item.Idle)
              } else {
                emptyList()
              },
              page = state.page + if (comics.isNotEmpty()) {
                1
              } else {
                0
              }
            )
          }
          Loading -> {
            state.copy(
              isLoading = false,
              errorMessage = null,
              comics = oldComics + Item.Loading
            )
          }
          is Error -> {
            state.copy(
              isLoading = false,
              errorMessage = null,
              comics = oldComics + Item.Error(this.error.getMessage())
            )
          }
        }
      }
    }
  }

  sealed class ViewIntent : MviIntent {
    data class SearchIntent(val term: String) : ViewIntent()
    object RetryFirstIntent : ViewIntent()

    object LoadNextPage : ViewIntent()
    object RetryNextPage : ViewIntent()
  }

  sealed class SingleEvent : MviSingleEvent {
    data class MessageEvent(val message: String) : SingleEvent()
  }
}
