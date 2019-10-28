package com.hoc.comicapp.data.firebase.favorite_comics

import com.hoc.comicapp.data.firebase.entity._FavoriteComic
import com.hoc.comicapp.utils.Either
import io.reactivex.Observable

interface FavoriteComicsDataSource {
  fun isFavorited(url: String): Observable<Either<Throwable, Boolean>>

  fun favoriteComics(): Observable<Either<Throwable, List<_FavoriteComic>>>

  suspend fun removeFromFavorite(comic: _FavoriteComic)

  suspend fun toggle(comic: _FavoriteComic)
}