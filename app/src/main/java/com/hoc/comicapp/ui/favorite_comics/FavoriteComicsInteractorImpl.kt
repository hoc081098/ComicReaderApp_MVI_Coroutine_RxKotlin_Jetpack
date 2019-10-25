package com.hoc.comicapp.ui.favorite_comics

import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ComicItem
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.Interactor
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.PartialChange
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable

class FavoriteComicsInteractorImpl(
  private val favoriteComicsRepository: FavoriteComicsRepository,
  private val rxSchedulerProvider: RxSchedulerProvider
) : Interactor {
  override fun getFavoriteComics(): Observable<PartialChange> {
    return favoriteComicsRepository
      .favoriteComics()
      .map<PartialChange> { either ->
        either.fold(
          left = { PartialChange.Error(it) },
          right = { PartialChange.Data(it.map(::ComicItem)) }
        )
      }
      .observeOn(rxSchedulerProvider.main)
      .startWith(PartialChange.Loading)
  }
}