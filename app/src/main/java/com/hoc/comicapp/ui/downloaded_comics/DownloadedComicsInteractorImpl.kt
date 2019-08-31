package com.hoc.comicapp.ui.downloaded_comics

import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.PartialChange
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable

class DownloadedComicsInteractorImpl(
  private val downloadComicsRepository: DownloadComicsRepository
) : DownloadedComicsContract.Interactor {
  override fun getDownloadedComics(): Observable<PartialChange> {
    return downloadComicsRepository
      .downloadedComics()
      .map<PartialChange> { result ->
        result.fold(
          left = { PartialChange.Error(it) },
          right = { list ->
            PartialChange.Data(
              comics = list.map {
                DownloadedComicsContract.ViewState.ComicItem(
                  title = it.title
                )
              }
            )
          }
        )
      }
      .startWith(PartialChange.Loading)
  }
}