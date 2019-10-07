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
  override fun searchComic(term: String, page: Int): Observable<PartialChange> {
    return rxObservable<PartialChange>(dispatcherProvider.ui) {
      if (page == 1) {
        send(PartialChange.FirstPage.Loading)
        comicRepository
          .searchComic(query = term)
          .fold(
            left = { PartialChange.FirstPage.Error(error = it, term = term) },
            right = { PartialChange.FirstPage.Data(comics = it.map(::ComicItem)) }
          )
          .let { send(it) }
      } else {
        TODO()
      }
    }
  }
}