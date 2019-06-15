package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.SearchComic
import com.hoc.comicapp.domain.models.getMessage
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope

interface SearchComicInteractor {
  fun searchComic(
    coroutineScope: CoroutineScope,
    term: String
  ): Observable<SearchComicPartialChange>
}

data class SearchComicViewState(
  val isLoading: Boolean,
  val comics: List<SearchComic>,
  val errorMessage: String?
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState() = SearchComicViewState(
      isLoading = false,
      comics = emptyList(),
      errorMessage = null
    )
  }
}

sealed class SearchComicPartialChange {
  fun reducer(state: SearchComicViewState): SearchComicViewState {
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
          errorMessage = null
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

  data class Data(val comics: List<SearchComic>) : SearchComicPartialChange()
  object Loading : SearchComicPartialChange()
  data class Error(val error: ComicAppError, val term: String) : SearchComicPartialChange()
}

sealed class SearchComicViewIntent : Intent {
  data class SearchIntent(val term: String) : SearchComicViewIntent()
  object RetryIntent : SearchComicViewIntent()
}

sealed class SearchComicSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : SearchComicSingleEvent()
}