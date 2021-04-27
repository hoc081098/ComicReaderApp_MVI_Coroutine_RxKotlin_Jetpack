package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.ui.search_comic.SearchComicContract.Interactor
import com.hoc.comicapp.ui.search_comic.SearchComicContract.PartialChange
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item.ComicItem
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.rxObservable
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class SearchComicInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
) : Interactor {
  override fun searchComic(term: String, page: Int): Observable<PartialChange> {
    return rxObservable(dispatchersProvider.main) {
      Timber.d("[INTERACTOR] $term $page")

      if (page == 1) {
        send(PartialChange.FirstPage.Loading)
        comicRepository
          .searchComic(query = term, page = page)
          .fold(
            ifLeft = { PartialChange.FirstPage.Error(error = it, term = term) },
            ifRight = { PartialChange.FirstPage.Data(comics = it.map(::ComicItem)) }
          )
          .let { send(it) }
      } else {
        send(PartialChange.NextPage.Loading)
        comicRepository
          .searchComic(query = term, page = page)
          .fold(
            ifLeft = { PartialChange.NextPage.Error(error = it, term = term) },
            ifRight = { PartialChange.NextPage.Data(comics = it.map(::ComicItem)) }
          )
          .let { send(it) }
      }
    }
  }
}
