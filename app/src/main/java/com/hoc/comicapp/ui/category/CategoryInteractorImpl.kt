package com.hoc.comicapp.ui.category

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.rxObservable

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
) : CategoryInteractor {
  override fun refresh(): Observable<CategoryPartialChange.RefreshPartialChange> {
    return rxObservable(dispatchersProvider.main) {
      send(CategoryPartialChange.RefreshPartialChange.Loading)
      comicRepository
        .getAllCategories()
        .fold(
          ifLeft = { CategoryPartialChange.RefreshPartialChange.Error(it) },
          ifRight = { CategoryPartialChange.RefreshPartialChange.Data(it) }
        )
        .let { send(it) }
    }
  }

  override fun getAllCategories(): Observable<CategoryPartialChange.InitialRetryPartialChange> {
    return rxObservable(dispatchersProvider.main) {
      send(CategoryPartialChange.InitialRetryPartialChange.Loading)
      comicRepository
        .getAllCategories()
        .fold(
          ifLeft = { CategoryPartialChange.InitialRetryPartialChange.Error(it) },
          ifRight = { CategoryPartialChange.InitialRetryPartialChange.Data(it) }
        )
        .let { send(it) }
    }
  }
}
