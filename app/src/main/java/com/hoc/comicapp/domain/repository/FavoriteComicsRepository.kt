package com.hoc.comicapp.domain.repository

import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.utils.Either
import io.reactivex.Observable

interface FavoriteComicsRepository {
  fun favoriteComics(): Observable<Either<ComicAppError, List<FavoriteComic>>>

  suspend fun addToFavorite(comic: FavoriteComic)

  suspend fun removeFromFavorite(comic: FavoriteComic)

  suspend fun toggle(comic: FavoriteComic)
}