package com.hoc.comicapp.ui.downloaded_comics

import android.app.Application
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.UnexpectedError
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.PartialChange
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem.Companion.fromDomain
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.rx3.rxSingle

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
          ifLeft = { PartialChange.Error(it) },
          ifRight = { list ->
            PartialChange.Data(
              comics = list.map { fromDomain(it, application) }
            )
          }
        )
      }
      .startWithItem(PartialChange.Loading)
  }
}
