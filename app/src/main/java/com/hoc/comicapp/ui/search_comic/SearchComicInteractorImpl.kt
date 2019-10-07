package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.search_comic.SearchComicContract.Interactor
import com.hoc.comicapp.ui.search_comic.SearchComicContract.PartialChange
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item.ComicItem
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class SearchComicInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : Interactor {
  override fun searchComic(term: String): Observable<PartialChange> {
    return rxObservable(dispatcherProvider.ui) {
      send(PartialChange.Loading)
      comicRepository
        .searchComic(query = term)
        .fold(
          left = { PartialChange.Error(error = it, term = term) },
          right = { PartialChange.Data(comics = it.map(::ComicItem)) }
        )
        .let { send(it) }
    }
  }
}