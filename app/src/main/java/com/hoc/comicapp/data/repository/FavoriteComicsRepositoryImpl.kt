package com.hoc.comicapp.data.repository

import arrow.core.right
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.Mappers
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import io.reactivex.rxjava3.core.Observable

class FavoriteComicsRepositoryImpl(
  private val errorMapper: ErrorMapper,
  private val favoriteComicsDataSource: FavoriteComicsDataSource,
) : FavoriteComicsRepository {

  override fun isFavorited(url: String): Observable<DomainResult<Boolean>> {
    return favoriteComicsDataSource
      .isFavorited(url)
      .map { either -> either.bimap(errorMapper::map) { it } }
  }

  override fun favoriteComics(): Observable<DomainResult<List<FavoriteComic>>> {
    return favoriteComicsDataSource
      .favoriteComics()
      .map { either ->
        either.bimap(errorMapper::map) { comics ->
          comics.map { it.toDomain() }
        }
      }
  }

  override suspend fun removeFromFavorite(comic: FavoriteComic): DomainResult<Unit> {
    return try {
      favoriteComicsDataSource.removeFromFavorite(Mappers.domainToFirebaseEntity(comic))
      Unit.right()
    } catch (e: Throwable) {
      errorMapper.mapAsLeft(e)
    }
  }

  override suspend fun toggle(comic: FavoriteComic): DomainResult<Unit> {
    return try {
      favoriteComicsDataSource.toggle(Mappers.domainToFirebaseEntity(comic))
      Unit.right()
    } catch (e: Throwable) {
      errorMapper.mapAsLeft(e)
    }
  }
}
