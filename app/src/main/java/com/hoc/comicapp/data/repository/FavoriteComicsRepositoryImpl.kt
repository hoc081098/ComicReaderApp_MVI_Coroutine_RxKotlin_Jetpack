package com.hoc.comicapp.data.repository

import com.hoc.comicapp.data.firebase.entity._FavoriteComic
import com.hoc.comicapp.data.firebase.favorite_comics.FavoriteComicsDataSource
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.models.toError
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.utils.bimap
import com.hoc.comicapp.utils.left
import com.hoc.comicapp.utils.right
import io.reactivex.Observable
import retrofit2.Retrofit

class FavoriteComicsRepositoryImpl(
  private val retrofit: Retrofit,
  private val favoriteComicsDataSource: FavoriteComicsDataSource
) : FavoriteComicsRepository {
  private fun FavoriteComic.toEntity(): _FavoriteComic {
    return _FavoriteComic(
      url = url,
      title = title,
      thumbnail = thumbnail,
      view = view,
      createdAt = null
    )
  }

  override fun isFavorited(url: String): Observable<Either<ComicAppError, Boolean>> {
    return favoriteComicsDataSource
      .isFavorited(url)
      .map { either ->
        either.bimap(
          { it.toError(retrofit) },
          { it }
        )
      }
  }

  override fun favoriteComics(): Observable<Either<ComicAppError, List<FavoriteComic>>> {
    return favoriteComicsDataSource
      .favoriteComics()
      .map { either ->
        either.bimap(
          { it.toError(retrofit) },
          { comics -> comics.map { it.toDomain() } }
        )
      }
  }

  override suspend fun removeFromFavorite(comic: FavoriteComic): Either<ComicAppError, Unit> {
    return try {
      favoriteComicsDataSource.removeFromFavorite(comic.toEntity())
      Unit.right()
    } catch (e: Throwable) {
      e.toError(retrofit).left()
    }
  }

  override suspend fun toggle(comic: FavoriteComic): Either<ComicAppError, Unit> {
    return try {
      favoriteComicsDataSource.toggle(comic.toEntity())
      Unit.right()
    } catch (e: Throwable) {
      e.toError(retrofit).left()
    }
  }
}
