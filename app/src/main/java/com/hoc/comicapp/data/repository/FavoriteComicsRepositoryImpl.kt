package com.hoc.comicapp.data.repository

import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.FavoriteComic
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.utils.Either
import io.reactivex.Observable

class FavoriteComicsRepositoryImpl : FavoriteComicsRepository {
  override fun favoriteComics(): Observable<Either<ComicAppError, List<FavoriteComic>>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}