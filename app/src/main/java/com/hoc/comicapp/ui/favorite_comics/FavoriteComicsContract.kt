package com.hoc.comicapp.ui.favorite_comics

import com.hoc.comicapp.base.MviIntent
import com.hoc.comicapp.base.MviSingleEvent
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.FavoriteComic
import io.reactivex.rxjava3.core.Observable
import java.util.Date

interface FavoriteComicsContract {
  sealed class ViewIntent : MviIntent {
    object Initial : ViewIntent()
    data class Remove(val item: ComicItem) : ViewIntent()
    data class ChangeSortOrder(val sortOrder: SortOrder) : ViewIntent()
  }

  data class ViewState(
    val isLoading: Boolean,
    val error: ComicAppError?,
    val comics: List<ComicItem>,
    val sortOrder: SortOrder,
  ) : com.hoc.comicapp.base.MviViewState {
    companion object {
      fun initial() = ViewState(
        isLoading = true,
        error = null,
        comics = emptyList(),
        sortOrder = SortOrder.ComicTitleAsc
      )
    }
  }

  enum class SortOrder(val comparator: Comparator<in ComicItem>, val description: String) {
    ComicTitleAsc(
      comparator = compareBy { it.title },
      description = "Title ascending"
    ),
    ComicTitleDesc(
      comparator = compareByDescending { it.title },
      description = "Title descending"
    ),
    CreatedAtAsc(
      comparator = nullsFirst(compareBy { it.createdAt }),
      description = "Added date - older first"
    ),
    CreatedAtDesc(
      comparator = nullsFirst(compareByDescending { it.createdAt }),
      description = "Added date - latest first"
    );

    override fun toString() = description
  }

  data class ComicItem(
    val url: String,
    val title: String,
    val thumbnail: String,
    val view: String,
    val createdAt: Date?,
  ) {
    fun toDomain() = FavoriteComic(
      url = url,
      view = view,
      createdAt = createdAt,
      thumbnail = thumbnail,
      title = title
    )

    constructor(domain: FavoriteComic) : this(
      url = domain.url,
      title = domain.title,
      thumbnail = domain.thumbnail,
      createdAt = domain.createdAt,
      view = domain.view
    )
  }

  sealed class SingleEvent : MviSingleEvent {
    data class Message(val message: String) : SingleEvent()
  }

  sealed class PartialChange {
    sealed class FavoriteComics : PartialChange() {
      data class Data(val comics: List<ComicItem>) : FavoriteComics()
      data class Error(val error: ComicAppError) : FavoriteComics()
      object Loading : FavoriteComics()
    }

    sealed class Remove : PartialChange() {
      data class Success(val item: ComicItem) : Remove()
      data class Failure(
        val item: ComicItem,
        val comicAppError: ComicAppError,
      ) : Remove()
    }
  }

  interface Interactor {
    fun getFavoriteComics(): Observable<PartialChange>
    fun remove(item: ComicItem): Observable<PartialChange>
  }
}
