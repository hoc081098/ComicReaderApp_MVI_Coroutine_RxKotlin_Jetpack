package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.domain.ComicRepository
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
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
    return coroutineScope.rxObservable<ComicDetailPartialChange> {
      send(ComicDetailPartialChange.RefreshPartialChange.Loading)

      comicRepository
        .getComicDetail(link)
        .fold(
          { ComicDetailPartialChange.RefreshPartialChange.Error(it) },
          { ComicDetailPartialChange.RefreshPartialChange.Success(ComicDetailViewState.ComicDetail.Comic(it)) }
        )
        .let { send(it) }
    }
  }

  override fun getComicDetail(
    coroutineScope: CoroutineScope,
    link: String,
    name: String?,
    thumbnail: String?
  ): Observable<ComicDetailPartialChange> {
    return coroutineScope.rxObservable<ComicDetailPartialChange> {

      if (thumbnail != null && name != null) {
        send(
          ComicDetailPartialChange.InitialRetryPartialChange.InitialData(
            initialComic = ComicDetailViewState.ComicDetail.InitialComic(
              link = link,
              thumbnail = thumbnail,
              title = name
            )
          )
        )
      }
      send(ComicDetailPartialChange.InitialRetryPartialChange.Loading)

      comicRepository
        .getComicDetail(link)
        .fold(
          { ComicDetailPartialChange.InitialRetryPartialChange.Error(it) },
          { ComicDetailPartialChange.InitialRetryPartialChange.Data(ComicDetailViewState.ComicDetail.Comic(it)) }
        )
        .let { send(it) }
    }
  }
}