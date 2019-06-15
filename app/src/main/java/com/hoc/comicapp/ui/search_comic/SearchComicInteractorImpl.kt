package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.domain.ComicRepository
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class SearchComicInteractorImpl(private val comicRepository: ComicRepository) : SearchComicInteractor {
  override fun searchComic(
    coroutineScope: CoroutineScope,
    term: String
  ): Observable<SearchComicPartialChange> {
    return coroutineScope.rxObservable {
      send(SearchComicPartialChange.Loading)
      comicRepository
        .searchComic(query = term)
        .fold(
          left = { SearchComicPartialChange.Error(error = it) },
          right = { SearchComicPartialChange.Data(comics = it) }
        )
        .let { send(it) }
    }
  }
}