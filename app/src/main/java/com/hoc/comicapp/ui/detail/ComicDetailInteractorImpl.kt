package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.utils.fold
import com.hoc.domain.ComicRepository
import io.reactivex.Observable
import io.reactivex.rxkotlin.cast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class ComicDetailInteractorImpl(private val comicRepository: ComicRepository) :
  ComicDetailInteractor {
  override fun refreshPartialChanges(
    coroutineScope: CoroutineScope,
    link: String
  ): Observable<ComicDetailPartialChange> {
    return coroutineScope.rxObservable {
      send(ComicDetailPartialChange.RefreshPartialChange.Loading)

      comicRepository
        .getComicDetail(link)
        .fold(
          { ComicDetailPartialChange.RefreshPartialChange.Error(it) },
          { ComicDetailPartialChange.RefreshPartialChange.Success(ComicDetailViewState.ComicDetail.Comic(it)) }
        )
        .let { send(it) }
    }.cast()
  }

  override fun getComicDetail(
    coroutineScope: CoroutineScope,
    link: String,
    name: String,
    thumbnail: String
  ): Observable<ComicDetailPartialChange> {
    return coroutineScope.rxObservable<ComicDetailPartialChange> {
      send(
        ComicDetailPartialChange.InitialPartialChange.InitialData(
          initialComic = ComicDetailViewState.ComicDetail.InitialComic(
            link = link,
            thumbnail = thumbnail,
            title = name
          )
        )
      )
      send(ComicDetailPartialChange.InitialPartialChange.Loading)

      comicRepository
        .getComicDetail(link)
        .fold(
          { ComicDetailPartialChange.InitialPartialChange.Error(it) },
          { ComicDetailPartialChange.InitialPartialChange.Data(ComicDetailViewState.ComicDetail.Comic(it)) }
        )
        .let { send(it) }
    }
  }
}