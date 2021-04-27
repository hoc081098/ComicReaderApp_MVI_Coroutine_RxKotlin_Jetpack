package com.hoc.comicapp.ui.favorite_comics

import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ComicItem
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.Interactor
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.PartialChange
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.rxObservable

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteComicsInteractorImpl(
  private val favoriteComicsRepository: FavoriteComicsRepository,
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
) : Interactor {
  override fun remove(item: ComicItem): Observable<PartialChange> {
    return rxObservable(dispatchersProvider.main) {
      favoriteComicsRepository
        .removeFromFavorite(item.toDomain())
        .fold(
          ifLeft = { PartialChange.Remove.Failure(item, it) },
          ifRight = { PartialChange.Remove.Success(item) }
        )
        .let { send(it) }
    }
  }

  override fun getFavoriteComics(): Observable<PartialChange> {
    return favoriteComicsRepository
      .favoriteComics()
      .map<PartialChange> { either ->
        either.fold(
          ifLeft = { PartialChange.FavoriteComics.Error(it) },
          ifRight = { PartialChange.FavoriteComics.Data(it.map(::ComicItem)) }
        )
      }
      .observeOn(rxSchedulerProvider.main)
      .startWithItem(PartialChange.FavoriteComics.Loading)
  }
}
