package com.hoc.comicapp.ui.category

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class CategoryInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : CategoryInteractor {
  override fun refresh(): Observable<CategoryPartialChange.RefreshPartialChange> {
    return rxObservable(dispatcherProvider.ui) {
      send(CategoryPartialChange.RefreshPartialChange.Loading)
      comicRepository
        .getAllCategories()
        .fold(
          left = { CategoryPartialChange.RefreshPartialChange.Error(it) },
          right = { CategoryPartialChange.RefreshPartialChange.Data(it) }
        )
        .let { send(it) }
    }
  }

  override fun getAllCategories(): Observable<CategoryPartialChange.InitialRetryPartialChange> {
    return rxObservable(dispatcherProvider.ui) {
      send(CategoryPartialChange.InitialRetryPartialChange.Loading)
      comicRepository
        .getAllCategories()
        .fold(
          left = { CategoryPartialChange.InitialRetryPartialChange.Error(it) },
          right = { CategoryPartialChange.InitialRetryPartialChange.Data(it) }
        )
        .let { send(it) }
    }
  }
}