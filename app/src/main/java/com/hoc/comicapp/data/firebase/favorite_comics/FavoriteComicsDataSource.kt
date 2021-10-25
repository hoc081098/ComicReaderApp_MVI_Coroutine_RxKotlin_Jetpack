package com.hoc.comicapp.data.firebase.favorite_comics

import arrow.core.Either
import com.hoc.comicapp.data.firebase.entity._FavoriteComic
import io.reactivex.rxjava3.core.Observable

interface FavoriteComicsDataSource {
  fun isFavorited(url: String): Observable<Either<Throwable, Boolean>>

  fun favoriteComics(): Observable<Either<Throwable, List<_FavoriteComic>>>

  suspend fun removeFromFavorite(comic: _FavoriteComic): Either<Throwable, Unit>

  suspend fun toggle(comic: _FavoriteComic): Either<Throwable, Unit>

  fun update(comics: List<_FavoriteComic>)
}
