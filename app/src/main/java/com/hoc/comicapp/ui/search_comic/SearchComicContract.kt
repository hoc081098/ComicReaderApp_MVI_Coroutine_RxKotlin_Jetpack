package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState

data class SearchComic(
  val latestChapterName: String,
  val categoriesName: List<String>,
  val link: String,
  val thumbnail: String,
  val title: String
)

data class SearchComicViewState(
  val isLoading: Boolean,
  val comics: List<SearchComic>,
  val error: Throwable?
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState() = SearchComicViewState(
      isLoading = true,
      comics = emptyList(),
      error = null
    )
  }
}

sealed class SearchComicPartialChange {
  abstract fun reducer(state: SearchComicViewState): SearchComicViewState
}

sealed class SearchComicViewIntent : Intent {

}

sealed class SearchComicSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : SearchComicSingleEvent()
}