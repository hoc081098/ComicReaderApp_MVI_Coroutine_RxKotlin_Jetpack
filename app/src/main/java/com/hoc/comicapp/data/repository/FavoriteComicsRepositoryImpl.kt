package com.hoc.comicapp.data.repository

import com.hoc.comicapp.data.Mapper
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.utils.bimap
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import io.reactivex.Observable
import retrofit2.Retrofit

class FavoriteComicsRepositoryImpl(
  private val retrofit: Retrofit,
  private val favoriteComicsDataSource: FavoriteComicsDataSource,
) : FavoriteComicsRepository {

  override fun isFavorited(url: String): Observable<DomainResult<Boolean>> {
    return favoriteComicsDataSource
      .isFavorited(url)
      .map { either ->
        either.bimap(
          { it.toError(retrofit) },
          { it }
        )
      }
  }

  override fun favoriteComics(): Observable<DomainResult<List<FavoriteComic>>> {
    return favoriteComicsDataSource
      .favoriteComics()
      .map { either ->
        either.bimap(
          { it.toError(retrofit) },
          { comics -> comics.map { it.toDomain() } }
        )
      }
  }

  override suspend fun removeFromFavorite(comic: FavoriteComic): DomainResult<Unit> {
    return try {
      favoriteComicsDataSource.removeFromFavorite(Mapper.domainToFirebaseEntity(comic))
      Unit.right()
    } catch (e: Throwable) {
      e.toError(retrofit).left()
    }
  }

  override suspend fun toggle(comic: FavoriteComic): DomainResult<Unit> {
    return try {
      favoriteComicsDataSource.toggle(Mapper.domainToFirebaseEntity(comic))
      Unit.right()
    } catch (e: Throwable) {
      e.toError(retrofit).left()
    }
  }
}
