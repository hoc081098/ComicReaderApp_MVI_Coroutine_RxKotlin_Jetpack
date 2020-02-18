package com.hoc.comicapp.ui.favorite_comics

import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ComicItem
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.Interactor
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.PartialChange
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class FavoriteComicsInteractorImpl(
  private val favoriteComicsRepository: FavoriteComicsRepository,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val dispatchersProvider: CoroutinesDispatchersProvider
) : Interactor {
  override fun remove(item: ComicItem): Observable<PartialChange> {
    return rxObservable<PartialChange>(dispatchersProvider.main) {
      favoriteComicsRepository
        .removeFromFavorite(item.toDomain())
        .fold(
          left = { PartialChange.Remove.Failure(item, it) },
          right = { PartialChange.Remove.Success(item) }
        )
        .let { send(it) }
    }
  }

  override fun getFavoriteComics(): Observable<PartialChange> {
    return favoriteComicsRepository
      .favoriteComics()
      .map<PartialChange> { either ->
        either.fold(
          left = { PartialChange.FavoriteComics.Error(it) },
          right = { PartialChange.FavoriteComics.Data(it.map(::ComicItem)) }
        )
      }
      .observeOn(rxSchedulerProvider.main)
      .startWith(PartialChange.FavoriteComics.Loading)
  }
}