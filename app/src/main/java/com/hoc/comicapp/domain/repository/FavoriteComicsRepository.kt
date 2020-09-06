package com.hoc.comicapp.domain.repository

import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.FavoriteComic
import io.reactivex.rxjava3.core.Observable

interface FavoriteComicsRepository {
  fun favoriteComics(): Observable<DomainResult<List<FavoriteComic>>>

  fun isFavorited(url: String): Observable<DomainResult<Boolean>>

  suspend fun removeFromFavorite(comic: FavoriteComic): DomainResult<Unit>

  suspend fun toggle(comic: FavoriteComic): DomainResult<Unit>
}
