package com.hoc.comicapp.data.repository

import arrow.core.identity
import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.data.Mappers
import com.hoc.comicapp.data.firebase.entity._FavoriteComic
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import io.reactivex.rxjava3.core.Observable
import timber.log.Timber

class FavoriteComicsRepositoryImpl(
  private val errorMapper: ErrorMapper,
  private val favoriteComicsDataSource: FavoriteComicsDataSource,
) : FavoriteComicsRepository {

  override fun isFavorited(url: String): Observable<DomainResult<Boolean>> =
    favoriteComicsDataSource
      .isFavorited(url)
      .doOnNext { either ->
        either.tapLeft {
          Timber.e(it, "Error occurred when observe favorite state $url")
        }
      }
      .map { it.bimap(errorMapper, ::identity) }

  override fun favoriteComics(): Observable<DomainResult<List<FavoriteComic>>> =
    favoriteComicsDataSource
      .favoriteComics()
      .doOnNext { either ->
        either.tapLeft {
          Timber.e(it, "Error occurred when observe favorite comics")
        }
      }
      .map { either ->
        either.bimap(errorMapper) { it.map(_FavoriteComic::toDomain) }
      }

  override suspend fun removeFromFavorite(comic: FavoriteComic) =
    favoriteComicsDataSource
      .removeFromFavorite(Mappers.domainToFirebaseEntity(comic))
      .tapLeft { Timber.e(it, "Error when remove comic from favorite $comic") }
      .mapLeft(errorMapper)

  override suspend fun toggle(comic: FavoriteComic) =
    favoriteComicsDataSource.toggle(Mappers.domainToFirebaseEntity(comic))
      .tapLeft { Timber.e(it, "Error when toggle comic $comic") }
      .mapLeft(errorMapper)
}
