package com.hoc.comicapp.ui.category

import com.hoc.comicapp.domain.ComicRepository
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class CategoryInteractorImpl(private val comicRepository: ComicRepository) : CategoryInteractor {
  override fun getAllCategories(coroutineScope: CoroutineScope): Observable<CategoryPartialChange> {
    return coroutineScope.rxObservable {
      send(CategoryPartialChange.Loading)
      comicRepository
        .getAllCategories()
        .fold(
          left = { CategoryPartialChange.Error(it) },
          right = { CategoryPartialChange.Data(it) }
        )
        .let { send(it) }
    }
  }
}