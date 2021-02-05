package com.hoc.comicapp.ui.category_detail

import com.hoc.comicapp.domain.models.CategoryDetailPopularComic
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.Item
import io.reactivex.rxjava3.core.Observable

/**
 * Contract
 */
interface CategoryDetailContract {
  /**
   * View intent
   */
  sealed class ViewIntent : com.hoc.comicapp.base.MviIntent {
    data class Initial(val arg: Arguments.CategoryDetailArgs) : ViewIntent()
    object Refresh : ViewIntent()
    object LoadNextPage : ViewIntent()
    object RetryPopular : ViewIntent()
    object Retry : ViewIntent()
  }

  /**
   * View state
   */
  data class ViewState(
    val items: List<Item>,
    val isRefreshing: Boolean,
    val page: Int,
    val category: Arguments.CategoryDetailArgs,
  ) : com.hoc.comicapp.base.MviViewState {
    companion object {
      @JvmStatic
      fun initial(category: Arguments.CategoryDetailArgs): ViewState {
        return ViewState(
          items = listOf(
            Item.Header(HeaderType.Popular),
            Item.PopularVS(
              isLoading = true,
              error = null,
              comics = emptyList()
            ),
            Item.Header(HeaderType.Updated),
            Item.Loading
          ),
          isRefreshing = false,
          page = 0,
          category = category
        )
      }
    }

    sealed class Item {
      fun isLoadingOrError() = this is Loading || this is Error

      data class PopularVS(
        val comics: List<PopularItem>,
        val error: ComicAppError?,
        val isLoading: Boolean,
      ) : Item()

      data class Comic(
        val lastChapters: List<LastChapter>,
        val link: String,
        val thumbnail: String,
        val title: String,
        val view: String,
      ) : Item() {
        constructor(domain: com.hoc.comicapp.domain.models.Comic) : this(
          title = domain.title,
          thumbnail = domain.thumbnail,
          link = domain.link,
          view = domain.view,
          lastChapters = domain.lastChapters.map(::LastChapter)
        )
      }

      object Loading : Item()

      data class Error(val error: ComicAppError) : Item()

      data class Header(val type: HeaderType) : Item()
    }

    data class PopularItem(
      val lastChapter: LastChapter,
      val link: String,
      val thumbnail: String,
      val title: String,
    ) {
      constructor(domain: CategoryDetailPopularComic) : this(
        title = domain.title,
        link = domain.link,
        thumbnail = domain.thumbnail,
        lastChapter = LastChapter(domain.lastChapter)
      )
    }

    data class LastChapter(
      val chapterLink: String,
      val chapterName: String,
      val time: String?,
    ) {
      constructor(domain: CategoryDetailPopularComic.LastChapter) : this(
        time = null,
        chapterName = domain.chapterName,
        chapterLink = domain.chapterLink
      )

      constructor(domain: Comic.LastChapter) : this(
        time = domain.time,
        chapterLink = domain.chapterLink,
        chapterName = domain.chapterName
      )
    }

    enum class HeaderType { Popular, Updated }
  }

  /**
   * Partial change
   */
  sealed class PartialChange {
    abstract fun reducer(state: ViewState): ViewState

    sealed class Popular : PartialChange() {
      object Loading : Popular()
      data class Error(val error: ComicAppError) : Popular()
      data class Data(val comics: List<ViewState.PopularItem>) : Popular()

      override fun reducer(state: ViewState): ViewState {
        return when (this) {
          Loading -> {
            state.copy(
              items = state.items.map {
                if (it is Item.PopularVS) {
                  it.copy(
                    isLoading = true,
                    error = null
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
                if (it is Item.PopularVS) {
                  it.copy(
                    isLoading = false,
                    error = this.error
                  )
                } else {
                  it
                }
              }
            )
          }
          is Data -> {
            state.copy(
              items = state.items.map {
                if (it is Item.PopularVS) {
                  it.copy(
                    isLoading = false,
                    error = null,
                    comics = this.comics
                  )
                } else {
                  it
                }
              }
            )
          }
        }
      }
    }

    sealed class ListComics : PartialChange() {
      object Loading : ListComics()
      data class Error(val error: ComicAppError) : ListComics()
      data class Data(val comics: List<Item.Comic>, val append: Boolean = true) : ListComics()

      override fun reducer(state: ViewState): ViewState {
        return when (this) {
          Loading -> {
            state.copy(
              items = state.items.filterNot(Item::isLoadingOrError) +
                Item.Loading
            )
          }
          is Error -> {
            state.copy(
              items = state.items.filterNot(Item::isLoadingOrError) +
                Item.Error(this.error)
            )
          }
          is Data -> {
            val newData = this.comics

            state.copy(
              items = if (this.append) {
                state.items.filterNot(Item::isLoadingOrError) + newData
              } else {
                state.items
                  .filterNot(Item::isLoadingOrError)
                  .filter { it !is Item.Comic } + newData
              },
              page = if (this.append) {
                state.page + 1
              } else {
                1
              }
            )
          }
        }
      }
    }

    sealed class Refresh : PartialChange() {
      object Loading : Refresh()
      data class Error(val error: ComicAppError) : Refresh()
      data class Data(
        val comics: List<Item.Comic>,
        val popularComics: List<ViewState.PopularItem>,
      ) : Refresh()

      override fun reducer(state: ViewState): ViewState {
        return when (this) {
          Loading -> {
            state.copy(isRefreshing = true)
          }
          is Error -> {
            state.copy(
              isRefreshing = false
            )
          }
          is Data -> {
            arrayOf(Popular.Data(this.popularComics), ListComics.Data(this.comics, append = false))
              .fold(state) { acc, change -> change.reducer(acc) }
              .copy(isRefreshing = false)
          }
        }
      }
    }
  }

  /**
   * Single event
   */
  sealed class SingleEvent : com.hoc.comicapp.base.MviSingleEvent

  /**
   * Interactor
   */
  interface Interactor {
    fun getPopulars(categoryLink: String): Observable<PartialChange>

    fun getComics(categoryLink: String, page: Int): Observable<PartialChange>

    fun refreshAll(categoryLink: String): Observable<PartialChange>
  }
}
