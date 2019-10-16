package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.InitialRetryLoadChapterPartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.RefreshPartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Chapter
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Detail
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Detail.Companion.fromDomain
import com.hoc.comicapp.utils.fold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asObservable
import timber.log.Timber

@ExperimentalCoroutinesApi
class ChapterDetailInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider,
  private val downloadComicsRepository: DownloadComicsRepository
) : ChapterDetailInteractor {
  override fun getChapterDetail(chapter: Chapter, isDownloaded: Boolean) = flow<InitialRetryLoadChapterPartialChange> {
    Timber.tag("LoadChapter###").d("getChapterDetail ${chapter.debug}")

    val initial = Detail.Initial(chapter)
    emit(InitialRetryLoadChapterPartialChange.InitialData(initial))

    emit(InitialRetryLoadChapterPartialChange.Loading)

    if (isDownloaded) {
      downloadComicsRepository
        .getDownloadedChapter(chapter.link)
        .map {
          it.fold(
            left = { InitialRetryLoadChapterPartialChange.Error(it, initial) },
            right = { InitialRetryLoadChapterPartialChange.Data(fromDomain(it)) }
          )
        }
        .let { emitAll(it) }
    } else {
      comicRepository
        .getChapterDetail(chapter.link)
        .fold(
          left = {
            InitialRetryLoadChapterPartialChange.Error(
              it,
              initial
            )
          },
          right = { InitialRetryLoadChapterPartialChange.Data(fromDomain(it)) }
        )
        .let { emit(it) }
    }
  }.flowOn(dispatcherProvider.ui).asObservable()

  override fun refresh(chapter: Chapter, isDownloaded: Boolean) = flow<ChapterDetailPartialChange> {
    Timber.tag("LoadChapter###").d("refresh ${chapter.debug}")

    emit(RefreshPartialChange.Loading)

    if (isDownloaded) {
      var isFirstEvent = true
      downloadComicsRepository
        .getDownloadedChapter(chapter.link)
        .map {
          if (isFirstEvent) {
            it.fold(
              left = { RefreshPartialChange.Error(it) },
              right = { RefreshPartialChange.Success(fromDomain(it)) }
            ).also { isFirstEvent = false }
          } else {
            val initial = Detail.Initial(chapter)
            it.fold(
              left = { InitialRetryLoadChapterPartialChange.Error(it, initial) },
              right = { InitialRetryLoadChapterPartialChange.Data(fromDomain(it)) }
            )
          }
        }
        .let { emitAll(it) }
    } else {
      comicRepository
        .getChapterDetail(chapter.link)
        .fold(
          left = { RefreshPartialChange.Error(it) },
          right = { RefreshPartialChange.Success(fromDomain(it)) }
        )
        .let { emit(it) }
    }
  }.flowOn(dispatcherProvider.ui).asObservable()
}