package com.hoc.comicapp.ui.downloaded_comics

import android.app.Application
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.UnexpectedError
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.PartialChange
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem.Companion.fromDomain
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.rx2.rxSingle

class DownloadedComicsInteractorImpl(
  private val downloadComicsRepository: DownloadComicsRepository,
  private val application: Application,
) : DownloadedComicsContract.Interactor {
  override fun deleteComic(comicItem: ComicItem): Single<Pair<ComicItem, ComicAppError?>> {
    return rxSingle {
      downloadComicsRepository
        .deleteComic(comicItem.toDomain())
        .fold(
          { comicItem to it },
          { comicItem to null }
        )
    }.onErrorReturn { comicItem to UnexpectedError(it.message ?: "", it) }
  }

  override fun getDownloadedComics(): Observable<PartialChange> {
    return downloadComicsRepository
      .getDownloadedComics()
      .map { result ->
        result.fold(
          left = { PartialChange.Error(it) },
          right = { list ->
            PartialChange.Data(
              comics = list.map { fromDomain(it, application) }
            )
          }
        )
      }
      .startWith(PartialChange.Loading)
  }
}