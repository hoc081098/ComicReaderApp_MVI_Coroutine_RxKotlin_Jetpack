package com.hoc.comicapp.ui.category_detail

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.Interactor
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange.ListComics
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange.Popular
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.Item.Comic
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.PopularItem
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class CategoryDetailInteractorImpl(
  private val dispatcherProvider: CoroutinesDispatcherProvider,
  private val comicRepository: ComicRepository
) : Interactor {
  override fun getPopulars(categoryLink: String): Observable<PartialChange> {
    return rxObservable<PartialChange>(dispatcherProvider.ui) {
      send(Popular.Loading)
      comicRepository
        .getCategoryDetailPopular(categoryLink)
        .fold(
          left = { Popular.Error(error = it) },
          right = { Popular.Data(comics = it.map(::PopularItem)) }
        )
        .let { send(it) }
    }
  }

  override fun getComics(categoryLink: String, page: Int): Observable<PartialChange> {
    return rxObservable<PartialChange>(dispatcherProvider.ui) {
      send(ListComics.Loading)
      comicRepository
        .getCategoryDetail(
          categoryLink = categoryLink,
          page = page
        )
        .fold(
          left = { ListComics.Error(error = it) },
          right = { ListComics.Data(comics = it.map(::Comic)) }
        )
        .let { send(it) }
    }
  }
}