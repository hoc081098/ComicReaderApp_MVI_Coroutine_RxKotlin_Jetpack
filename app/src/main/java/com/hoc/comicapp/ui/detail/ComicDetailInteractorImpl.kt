package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.detail.ComicDetailPartialChange.InitialRetryPartialChange
import com.hoc.comicapp.ui.detail.ComicDetailPartialChange.RefreshPartialChange
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail.Detail
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail.Initial
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.collect
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class ComicDetailInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider,
  private val downloadedComicRepository: DownloadComicsRepository,
  private val favoriteComicsRepository: FavoriteComicsRepository,
  private val rxSchedulerProvider: RxSchedulerProvider
) : ComicDetailInteractor {
  override fun toggleFavorite(comic: Detail): Observable<Unit> {
    return rxObservable(dispatcherProvider.ui) {
      favoriteComicsRepository.toggle(comic.toDomain())
      send(Unit)
    }
  }

  override fun getFavoriteChange(link: String): Observable<ComicDetailPartialChange> {
    return favoriteComicsRepository
      .isFavorited(link)
      .map<ComicDetailPartialChange> { either ->
        either.fold(
          left = { ComicDetailPartialChange.FavoriteChange(null) },
          right = { ComicDetailPartialChange.FavoriteChange(it) }
        )
      }
      .observeOn(rxSchedulerProvider.main)
  }

  override fun refreshPartialChanges(
    link: String,
    isDownloaded: Boolean
  ): Observable<ComicDetailPartialChange> {
    return if (isDownloaded) {
      rxObservable<ComicDetailPartialChange> {
        send(RefreshPartialChange.Loading)

        downloadedComicRepository
          .getDownloadedComic(link)
          .map {
            it.fold(
              { RefreshPartialChange.Error(it) },
              { RefreshPartialChange.Success(it.toViewComicDetail()) }
            )
          }
          .collect { send(it) }
      }
    } else {
      rxObservable<ComicDetailPartialChange>(dispatcherProvider.ui) {
        send(RefreshPartialChange.Loading)

        comicRepository
          .getComicDetail(link)
          .fold(
            { RefreshPartialChange.Error(it) },
            { RefreshPartialChange.Success(it.toViewComicDetail()) }
          )
          .let { send(it) }
      }
    }
  }

  override fun getComicDetail(
    link: String,
    name: String?,
    thumbnail: String?,
    isDownloaded: Boolean
  ): Observable<ComicDetailPartialChange> {
    return if (isDownloaded) {
      rxObservable<ComicDetailPartialChange>(dispatcherProvider.ui) {
        if (thumbnail != null && name != null) {
          send(
            InitialRetryPartialChange.InitialData(
              initialComic = Initial(
                link = link,
                thumbnail = thumbnail,
                title = name
              )
            )
          )
        }
        send(InitialRetryPartialChange.Loading)
        downloadedComicRepository
          .getDownloadedComic(link)
          .map {
            it.fold(
              { InitialRetryPartialChange.Error(it) },
              { InitialRetryPartialChange.Data(it.toViewComicDetail()) }
            )
          }
          .collect { send(it) }
      }
    } else {
      rxObservable<ComicDetailPartialChange>(dispatcherProvider.ui) {

        if (thumbnail != null && name != null) {
          send(
            InitialRetryPartialChange.InitialData(
              initialComic = Initial(
                link = link,
                thumbnail = thumbnail,
                title = name
              )
            )
          )
        }
        send(InitialRetryPartialChange.Loading)

        comicRepository
          .getComicDetail(link)
          .fold(
            { InitialRetryPartialChange.Error(it) },
            { InitialRetryPartialChange.Data(it.toViewComicDetail()) }
          )
          .let { send(it) }
      }
    }
  }
}

private fun DownloadedComic.toViewComicDetail(): Detail {
  return Detail(
    link = comicLink,
    thumbnail = thumbnail,
    title = title,
    shortenedContent = shortenedContent,
    lastUpdated = lastUpdated,
    view = view,
    authors = authors.map {
      ComicDetailViewState.Author(
        name = it.name,
        link = it.name
      )
    },
    chapters = chapters.map {
      ComicDetailViewState.Chapter(
        chapterName = it.chapterName,
        chapterLink = it.chapterLink,
        view = it.view,
        comicLink = it.comicLink,
        time = it.time,
        downloadState = ComicDetailViewState.DownloadState.Downloaded
      )
    },
    categories = categories.map {
      ComicDetailViewState.Category(
        name = it.name,
        link = it.link
      )
    },
    relatedComics = emptyList()
  )
}

private fun ComicDetail.toViewComicDetail(): Detail {
  return Detail(
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
