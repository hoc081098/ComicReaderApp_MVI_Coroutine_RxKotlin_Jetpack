package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.repository.FavoriteComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.detail.ComicDetailPartialChange.InitialRetryPartialChange
import com.hoc.comicapp.ui.detail.ComicDetailPartialChange.RefreshPartialChange
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail.Detail
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail.Initial
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx3.collect
import kotlinx.coroutines.rx3.rxObservable

@OptIn(ExperimentalCoroutinesApi::class)
class ComicDetailInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val downloadedComicRepository: DownloadComicsRepository,
  private val favoriteComicsRepository: FavoriteComicsRepository,
  private val rxSchedulerProvider: RxSchedulerProvider,
) : ComicDetailInteractor {
  override fun deleteOrCancelDownload(chapter: ComicDetailViewState.Chapter): Observable<ComicDetailSingleEvent> {
    return rxObservable(dispatchersProvider.main) {
      downloadedComicRepository
        .deleteDownloadedChapter(chapter = chapter.toDownloadedChapterDomain())
        .fold(
          { ComicDetailSingleEvent.DeleteChapterError(chapter, it) },
          { ComicDetailSingleEvent.DeletedChapter(chapter) }
        )
        .let { send(it) }
    }
  }

  override fun enqueueDownloadComic(
    chapter: ComicDetailViewState.Chapter,
    comicName: String,
    comicLink: String,
  ): Observable<ComicDetailSingleEvent> {
    return rxObservable(dispatchersProvider.main) {
      downloadedComicRepository
        .enqueueDownload(
          chapter = chapter.toDownloadedChapterDomain(),
          comicName = comicName,
          comicLink = comicLink,
        )
        .fold(
          { ComicDetailSingleEvent.EnqueuedDownloadFailure(chapter, it) },
          { ComicDetailSingleEvent.EnqueuedDownloadSuccess(chapter) }
        )
        .let { send(it) }
    }
  }

  override fun toggleFavorite(comic: ComicDetailViewState.ComicDetail): Observable<Unit> {
    return rxObservable(dispatchersProvider.main) {
      favoriteComicsRepository.toggle(comic.toDomain())
      send(Unit)
    }
  }

  override fun getFavoriteChange(link: String): Observable<ComicDetailPartialChange> {
    return favoriteComicsRepository
      .isFavorited(link)
      .map<ComicDetailPartialChange> { either ->
        either.fold(
          ifLeft = { ComicDetailPartialChange.FavoriteChange(null) },
          ifRight = { ComicDetailPartialChange.FavoriteChange(it) }
        )
      }
      .observeOn(rxSchedulerProvider.main)
  }

  override fun refreshPartialChanges(
    link: String,
    isDownloaded: Boolean,
  ): Observable<ComicDetailPartialChange> {
    return if (isDownloaded) {
      rxObservable<ComicDetailPartialChange>(dispatchersProvider.main) {
        send(RefreshPartialChange.Loading)

        downloadedComicRepository
          .getDownloadedComic(link)
          .map { result ->
            result.fold(
              { RefreshPartialChange.Error(it) },
              { RefreshPartialChange.Success(it.toViewComicDetail()) }
            )
          }
          .collect { send(it) }
      }
    } else {
      rxObservable<ComicDetailPartialChange>(dispatchersProvider.main) {
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
    view: String?,
    remoteThumbnail: String?,
    isDownloaded: Boolean,
  ) = if (isDownloaded) {
    _getDownloadedComicDetail(thumbnail, name, view, remoteThumbnail, link)
  } else {
    _getComicDetail(thumbnail, name, view, remoteThumbnail, link)
  }

  @Suppress("FunctionName")
  private fun _getComicDetail(
    thumbnail: String?,
    name: String?,
    view: String?,
    remoteThumbnail: String?,
    link: String,
  ): Observable<ComicDetailPartialChange> {
    return rxObservable(dispatchersProvider.main) {
      if (thumbnail != null && name != null && view != null && remoteThumbnail != null) {
        send(
          InitialRetryPartialChange.InitialData(
            initialComic = Initial(
              link = link,
              thumbnail = thumbnail,
              title = name,
              view = view,
              remoteThumbnail = remoteThumbnail
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

  @Suppress("FunctionName")
  private fun _getDownloadedComicDetail(
    thumbnail: String?,
    name: String?,
    view: String?,
    remoteThumbnail: String?,
    link: String,
  ): Observable<ComicDetailPartialChange> {
    return rxObservable(dispatchersProvider.main) {
      if (thumbnail != null && name != null && view != null && remoteThumbnail != null) {
        send(
          InitialRetryPartialChange.InitialData(
            initialComic = Initial(
              link = link,
              thumbnail = thumbnail,
              title = name,
              view = view,
              remoteThumbnail = remoteThumbnail
            )
          )
        )
      }
      send(InitialRetryPartialChange.Loading)
      downloadedComicRepository
        .getDownloadedComic(link)
        .map { result ->
          result.fold(
            { InitialRetryPartialChange.Error(it) },
            { InitialRetryPartialChange.Data(it.toViewComicDetail()) }
          )
        }
        .collect { send(it) }
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
    relatedComics = emptyList(),
    remoteThumbnail = remoteThumbnail
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
    relatedComics = relatedComics.map { comic ->
      ComicDetailViewState.Comic(
        title = comic.title,
        thumbnail = comic.thumbnail,
        link = comic.link,
        view = comic.view,
        lastChapters = comic.lastChapters.map {
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
    },
    remoteThumbnail = thumbnail
  )
}
