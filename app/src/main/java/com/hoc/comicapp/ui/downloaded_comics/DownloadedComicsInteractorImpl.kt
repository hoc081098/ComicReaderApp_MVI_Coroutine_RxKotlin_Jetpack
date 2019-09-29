package com.hoc.comicapp.ui.downloaded_comics

import android.app.Application
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.PartialChange
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ChapterItem
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import java.io.File

class DownloadedComicsInteractorImpl(
  private val downloadComicsRepository: DownloadComicsRepository,
  private val application: Application
) : DownloadedComicsContract.Interactor {
  override fun getDownloadedComics(): Observable<PartialChange> {
    return downloadComicsRepository
      .getDownloadedComics()
      .map<PartialChange> { result ->
        result.fold(
          left = { PartialChange.Error(it) },
          right = { list ->
            PartialChange.Data(
              comics = list.map { comic ->
                ComicItem(
                  title = comic.title,
                  comicLink = comic.comicLink,
                  view = comic.view,
                  thumbnail = File(application.filesDir, comic.thumbnail),
                  chapters = comic.chapters.map {
                    ChapterItem(
                      chapterName = it.chapterName,
                      downloadedAt = it.downloadedAt
                    )
                  }
                )
              }
            )
          }
        )
      }
      .startWith(PartialChange.Loading)
  }
}