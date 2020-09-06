package com.hoc.comicapp.ui.home

import com.hoc.comicapp.base.MviIntent
import com.hoc.comicapp.base.MviSingleEvent
import com.hoc.comicapp.base.MviViewState
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import io.reactivex.rxjava3.core.Observable

interface HomeInteractor {
  fun newestComics(): Observable<HomePartialChange>

  fun mostViewedComics(): Observable<HomePartialChange>

  fun updatedComics(page: Int): Observable<HomePartialChange>

  fun refreshAll(): Observable<HomePartialChange>
}

sealed class HomeListItem {
  data class NewestListState(
    val comics: List<Comic>,
    val errorMessage: String?,
    val isLoading: Boolean,
  ) : HomeListItem()

  data class MostViewedListState(
    val comics: List<Comic>,
    val errorMessage: String?,
    val isLoading: Boolean,
  ) : HomeListItem()

  sealed class UpdatedItem : HomeListItem() {
    data class ComicItem(val comic: Comic) : UpdatedItem()
    data class Error(val errorMessage: String?) : UpdatedItem()
    object Loading : UpdatedItem()
  }

  data class Header(val type: HeaderType) : HomeListItem()

  enum class HeaderType { NEWEST, MOST_VIEWED, UPDATED }
}

data class HomeViewState(
  val items: List<HomeListItem>,
  val refreshLoading: Boolean,
  val updatedPage: Int,
) : MviViewState {
  companion object {
    @JvmStatic
    fun initialState() = HomeViewState(
      items = listOf(
        HomeListItem.Header(HomeListItem.HeaderType.NEWEST),
        HomeListItem.NewestListState(
          comics = emptyList(),
          isLoading = false,
          errorMessage = null
        ),
        HomeListItem.Header(HomeListItem.HeaderType.MOST_VIEWED),
        HomeListItem.MostViewedListState(
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

sealed class HomeViewIntent : MviIntent {
  object Initial : HomeViewIntent()
  object Refresh : HomeViewIntent()
  object LoadNextPageUpdatedComic : HomeViewIntent()
  object RetryNewest : HomeViewIntent()
  object RetryMostViewed : HomeViewIntent()
  object RetryUpdate : HomeViewIntent()
}

sealed class HomePartialChange {
  abstract fun reducer(state: HomeViewState): HomeViewState

  sealed class NewestHomePartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is Data -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.NewestListState) {
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
        Loading -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.NewestListState) {
                it.copy(
                  isLoading = true,
                  errorMessage = null
                )
              } else {
                it
              }
            }
          )
        }
        is Error -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.NewestListState) {
                it.copy(
                  isLoading = false,
                  errorMessage = this.error.getMessage()
                )
              } else {
                it
              }
            }
          )
        }
      }
    }

    data class Data(val comics: List<Comic>) : NewestHomePartialChange()
    object Loading : NewestHomePartialChange()
    data class Error(val error: ComicAppError) :
      NewestHomePartialChange()
  }

  sealed class MostViewedHomePartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is Data -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.MostViewedListState) {
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
        Loading -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.MostViewedListState) {
                it.copy(
                  isLoading = true,
                  errorMessage = null
                )
              } else {
                it
              }
            }
          )
        }
        is Error -> {
          state.copy(
            items = state.items.map {
              if (it is HomeListItem.MostViewedListState) {
                it.copy(
                  isLoading = false,
                  errorMessage = this.error.getMessage()
                )
              } else {
                it
              }
            }
          )
        }
      }
    }

    data class Data(val comics: List<Comic>) : MostViewedHomePartialChange()
    object Loading : MostViewedHomePartialChange()
    data class Error(val error: ComicAppError) :
      MostViewedHomePartialChange()
  }

  sealed class UpdatedPartialChange : HomePartialChange() {
    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is Data -> {
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
        Loading -> {
          state.copy(
            items = state.items.filterNot(HomeListItem::isLoadingOrError) +
              HomeListItem.UpdatedItem.Loading
          )
        }
        is Error -> {
          state.copy(
            items = state.items.filterNot(HomeListItem::isLoadingOrError) +
              HomeListItem.UpdatedItem.Error(this.error.getMessage())
          )
        }
      }
    }

    data class Data(val comics: List<Comic>, val append: Boolean = true) : UpdatedPartialChange()
    object Loading : UpdatedPartialChange()
    data class Error(val error: ComicAppError) : UpdatedPartialChange()
  }

  sealed class RefreshPartialChange : HomePartialChange() {

    override fun reducer(state: HomeViewState): HomeViewState {
      return when (this) {
        is RefreshSuccess -> {
          listOf(
            NewestHomePartialChange.Data(newestComics),
            MostViewedHomePartialChange.Data(mostViewedComics),
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
      val newestComics: List<Comic>,
      val mostViewedComics: List<Comic>,
      val updatedComics: List<Comic>,
    ) : RefreshPartialChange()

    object Loading : RefreshPartialChange()
    data class RefreshFailure(val error: ComicAppError) : RefreshPartialChange()
  }
}

sealed class HomeSingleEvent : MviSingleEvent {
  data class MessageEvent(val message: String) : HomeSingleEvent()
}

fun HomeListItem.isLoadingOrError(): Boolean {
  return this is HomeListItem.UpdatedItem.Loading || this is HomeListItem.UpdatedItem.Error
}
