package com.hoc.comicapp.ui.home

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.ComicAppError
import com.hoc.comicapp.data.models.getMessageFromError
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope

interface HomeInteractor {
  fun suggestComicsPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange>

  fun topMonthComicsPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange>

  fun updatedComicsPartialChanges(
    coroutineScope: CoroutineScope,
    page: Int
  ): Observable<HomePartialChange>

  fun refreshAllPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange>
}

sealed class HomeListItem {
  data class SuggestListState(
    val comics: List<Comic>,
    val errorMessage: String?,
    val isLoading: Boolean
  ) : HomeListItem()

  data class TopMonthListState(
    val comics: List<Comic>,
    val errorMessage: String?,
    val isLoading: Boolean
  ) : HomeListItem()

  sealed class UpdatedItem : HomeListItem() {
    data class ComicItem(val comic: Comic) : UpdatedItem()
    data class Error(val errorMessage: String?) : UpdatedItem()
    object Loading : UpdatedItem()
  }

  data class Header(val type: HeaderType) : HomeListItem()

  enum class HeaderType { SUGGEST, TOP_MONTH, UPDATED }
}

data class HomeViewState(
  val items: List<HomeListItem>,
  val refreshLoading: Boolean,
  val updatedPage: Int
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState() = HomeViewState(
      items = listOf(
        HomeListItem.Header(HomeListItem.HeaderType.SUGGEST),
        HomeListItem.SuggestListState(
          comics = emptyList(),
          isLoading = false,
          errorMessage = null
        ),
        HomeListItem.Header(HomeListItem.HeaderType.TOP_MONTH),
        HomeListItem.TopMonthListState(
          comics = emptyList(),
          isLoading = false,
          errorMessage = null
        ),
        HomeListItem.Header(HomeListItem.HeaderType.UPDATED),
        HomeListItem.UpdatedItem.Loading
      ),
      refreshLoading = false,
      updatedPage = 0
    )
  }
}

sealed class HomeViewIntent : Intent {
  object Initial : HomeViewIntent()
  object Refresh : HomeViewIntent()
  object LoadNextPageUpdatedComic : HomeViewIntent()
  object RetrySuggest : HomeViewIntent()
  object RetryTopMonth : HomeViewIntent()
  object RetryUpdate : HomeViewIntent()
}

sealed class HomePartialChange {
  abstract fun reducer(state: HomeViewState): HomeViewState

  sealed class SuggestHomePartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is HomePartialChange.SuggestHomePartialChange.Data -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.SuggestListState) {
                it.copy(
                  comics = this.comics,
                  isLoading = false,
                  errorMessage = null
                )
              } else {
                it
              }
            }
          )
        }
        HomePartialChange.SuggestHomePartialChange.Loading -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.SuggestListState) {
                it.copy(isLoading = true)
              } else {
                it
              }
            }
          )
        }
        is HomePartialChange.SuggestHomePartialChange.Error -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.SuggestListState) {
                it.copy(
                  isLoading = false,
                  errorMessage = getMessageFromError(this.error)
                )
              } else {
                it
              }
            }
          )
        }
      }
    }

    data class Data(val comics: List<Comic>) : SuggestHomePartialChange()
    object Loading : SuggestHomePartialChange()
    data class Error(val error: com.hoc.comicapp.data.models.ComicAppError) :
      SuggestHomePartialChange()
  }

  sealed class TopMonthHomePartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is HomePartialChange.TopMonthHomePartialChange.Data -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.TopMonthListState) {
                it.copy(
                  comics = this.comics,
                  isLoading = false,
                  errorMessage = null
                )
              } else {
                it
              }
            }
          )
        }
        HomePartialChange.TopMonthHomePartialChange.Loading -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.TopMonthListState) {
                it.copy(isLoading = true)
              } else {
                it
              }
            }
          )
        }
        is HomePartialChange.TopMonthHomePartialChange.Error -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.TopMonthListState) {
                it.copy(
                  isLoading = false,
                  errorMessage = getMessageFromError(this.error)
                )
              } else {
                it
              }
            }
          )
        }
      }
    }

    data class Data(val comics: List<Comic>) : TopMonthHomePartialChange()
    object Loading : TopMonthHomePartialChange()
    data class Error(val error: com.hoc.comicapp.data.models.ComicAppError) :
      TopMonthHomePartialChange()
  }

  sealed class UpdatedPartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is HomePartialChange.UpdatedPartialChange.Data -> {
          val newData = this.comics.map { HomeListItem.UpdatedItem.ComicItem(it) }

          state.copy(
            items = if (this.append) {
              state.items.filterNot(HomeListItem::isLoadingOrError) + newData
            } else {
              state.items.filterNot { it is HomeListItem.UpdatedItem } + newData
            },
            updatedPage = if (this.append) {
              state.updatedPage + 1
            } else {
              1
            }
          )
        }
        HomePartialChange.UpdatedPartialChange.Loading -> {
          state.copy(
            items = state.items.filterNot(HomeListItem::isLoadingOrError) +
              HomeListItem.UpdatedItem.Loading
          )
        }
        is HomePartialChange.UpdatedPartialChange.Error -> {
          state.copy(
            items = state.items.filterNot(HomeListItem::isLoadingOrError) +
              HomeListItem.UpdatedItem.Error(getMessageFromError(this.error))
          )
        }
      }
    }

    data class Data(val comics: List<Comic>, val append: Boolean = true) : UpdatedPartialChange()
    object Loading : UpdatedPartialChange()
    data class Error(val error: com.hoc.comicapp.data.models.ComicAppError) : UpdatedPartialChange()
  }

  sealed class RefreshPartialChange : HomePartialChange() {

    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is RefreshSuccess -> {
          listOf(
            SuggestHomePartialChange.Data(suggestComics),
            TopMonthHomePartialChange.Data(topMonthComics),
            UpdatedPartialChange.Data(updatedComics, append = false)
          ).fold(state) { acc, homePartialChange ->
            homePartialChange.reducer(acc)
          }.copy(refreshLoading = false)
        }
        is RefreshFailure -> {
          state.copy(refreshLoading = false)
        }
        is Loading -> {
          state.copy(refreshLoading = true)
        }
      }
    }

    data class RefreshSuccess(
      val suggestComics: List<Comic>,
      val topMonthComics: List<Comic>,
      val updatedComics: List<Comic>
    ) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
    data class RefreshFailure(val error: ComicAppError) : RefreshPartialChange()
  }
}

sealed class HomeSingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : HomeSingleEvent()
}

fun HomeListItem.isLoadingOrError(): Boolean {
  return this is HomeListItem.UpdatedItem.Loading || this is HomeListItem.UpdatedItem.Error
}