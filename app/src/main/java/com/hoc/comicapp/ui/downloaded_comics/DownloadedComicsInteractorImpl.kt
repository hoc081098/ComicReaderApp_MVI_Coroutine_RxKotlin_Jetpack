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
      .downloadedComics()
      .map<PartialChange> { result ->
        result.fold(
          left = { PartialChange.Error(it) },
          right = { list ->
            PartialChange.Data(
              comics = list.map {
                ComicItem(
                  title = it.title,
                  comicLink = it.comicLink,
                  view = it.view,
                  thumbnail = File(application.filesDir, it.thumbnail),
                  lastUpdated = it.lastUpdated,
                  chapters = it.chapters.map {
                    ChapterItem(
                      chapterName = it.chapterName,
                      time = it.time
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