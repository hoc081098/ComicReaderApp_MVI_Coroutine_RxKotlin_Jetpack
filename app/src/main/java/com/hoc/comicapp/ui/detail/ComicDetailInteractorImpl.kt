package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class ComicDetailInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) :
  ComicDetailInteractor {
  override fun refreshPartialChanges(link: String): Observable<ComicDetailPartialChange> {
    return rxObservable<ComicDetailPartialChange>(dispatcherProvider.ui) {
      send(ComicDetailPartialChange.RefreshPartialChange.Loading)

      comicRepository
        .getComicDetail(link)
        .fold(
          { ComicDetailPartialChange.RefreshPartialChange.Error(it) },
          { ComicDetailPartialChange.RefreshPartialChange.Success(it.toViewComicDetail()) }
        )
        .let { send(it) }
    }
  }

  override fun getComicDetail(
    link: String,
    name: String?,
    thumbnail: String?
  ): Observable<ComicDetailPartialChange> {
    return rxObservable<ComicDetailPartialChange>(dispatcherProvider.ui) {

      if (thumbnail != null && name != null) {
        send(
          ComicDetailPartialChange.InitialRetryPartialChange.InitialData(
            initialComic = ComicDetailViewState.ComicDetail.Initial(
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
          { ComicDetailPartialChange.InitialRetryPartialChange.Data(it.toViewComicDetail()) }
        )
        .let { send(it) }
    }
  }
}

private fun ComicDetail.toViewComicDetail(): ComicDetailViewState.ComicDetail.Detail {
  return ComicDetailViewState.ComicDetail.Detail(
    title = title,
    view = view,
    link = link,
    thumbnail = thumbnail,
    shortenedContent = shortenedContent,
    lastUpdated = lastUpdated,
    relatedComics = relatedComics.map {
      ComicDetailViewState.Comic(
        title = it.title,
        thumbnail = it.thumbnail,
        link = it.link,
        view = it.view,
        lastChapters = it.lastChapters.map {
          ComicDetailViewState.Comic.LastChapter(
            chapterName = it.chapterName,
            time = it.time,
            chapterLink = it.chapterLink
          )
        }
      )
    },
    chapters = chapters.map {
      ComicDetailViewState.Chapter(
        chapterLink = it.chapterLink,
        chapterName = it.chapterName,
        time = it.time,
        view = it.view,
        comicLink = link
      )
    },
    authors = authors.map {
      ComicDetailViewState.Author(
        name = it.name,
        link = it.link
      )
    },
    categories = categories.map {
      ComicDetailViewState.Category(
        name = it.name,
        link = it.link
      )
    }
  )
}
