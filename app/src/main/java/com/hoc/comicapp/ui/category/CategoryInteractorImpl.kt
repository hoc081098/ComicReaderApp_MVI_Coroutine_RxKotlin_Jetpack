package com.hoc.comicapp.ui.category

import com.hoc.comicapp.domain.ComicRepository
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class CategoryInteractorImpl(private val comicRepository: ComicRepository) : CategoryInteractor {
  override fun refresh(coroutineScope: CoroutineScope): Observable<CategoryPartialChange.RefreshPartialChange> {
    return coroutineScope.rxObservable {
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

  override fun getAllCategories(coroutineScope: CoroutineScope): Observable<CategoryPartialChange.InitialRetryPartialChange> {
    return coroutineScope.rxObservable {
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