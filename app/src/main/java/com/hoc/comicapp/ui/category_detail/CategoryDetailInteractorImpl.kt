package com.hoc.comicapp.ui.category_detail

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.Interactor
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange.ListComics
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange.Popular
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange.Refresh.Data
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange.Refresh.Error
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.PartialChange.Refresh.Loading
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.Item.Comic
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.PopularItem
import com.hoc.comicapp.utils.flatMap
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.map
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx3.rxObservable

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryDetailInteractorImpl(
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val comicRepository: ComicRepository,
) : Interactor {
  override fun refreshAll(categoryLink: String): Observable<PartialChange> {
    return rxObservable(dispatchersProvider.main) {
      coroutineScope {
        send(Loading)

        val popularsDeferred = async { comicRepository.getCategoryDetailPopular(categoryLink) }
        val comicsDeferred = async { comicRepository.getCategoryDetail(categoryLink, page = 1) }

        comicsDeferred
          .await()
          .flatMap { comics ->
            popularsDeferred
              .await()
              .map { populars ->
                Data(
                  comics = comics.map(::Comic),
                  popularComics = populars.map(::PopularItem)
                )
              }
          }
          .fold(
            left = { Error(it) },
            right = { it }
          )
          .let { send(it) }
      }
    }
  }

  override fun getPopulars(categoryLink: String): Observable<PartialChange> {
    return rxObservable(dispatchersProvider.main) {
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
    return rxObservable(dispatchersProvider.main) {
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