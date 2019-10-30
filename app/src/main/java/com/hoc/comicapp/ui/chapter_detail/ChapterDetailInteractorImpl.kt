package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.Interactor
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.PartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState.Detail.Companion.fromDomain
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
) : Interactor {
  override fun getChapterDetail(chapter: ViewState.Chapter, isDownloaded: Boolean) = flow<PartialChange> {
    Timber.tag("LoadChapter###").d("getChapterDetail ${chapter.debug}")

    val initial = ViewState.Detail.Initial(chapter)
    emit(PartialChange.InitialRetryLoadChapterPartialChange.InitialData(initial))

    emit(PartialChange.InitialRetryLoadChapterPartialChange.Loading)

    if (isDownloaded) {
      downloadComicsRepository
        .getDownloadedChapter(chapter.link)
        .map {
          it.fold(
            left = {
              PartialChange.InitialRetryLoadChapterPartialChange.Error(
                it,
                ViewState.Detail.Data(
                  TODO(),
                  TODO(),
                  TODO(),
                  TODO(),
                  TODO()
                )
              )
            },
            right = { PartialChange.InitialRetryLoadChapterPartialChange.Data(fromDomain(it)) }
          )
        }
        .let { emitAll(it) }
    } else {
      comicRepository
        .getChapterDetail(chapter.link)
        .fold(
          left = {
            PartialChange.InitialRetryLoadChapterPartialChange.Error(
              it,
              TODO()
            )
          },
          right = { PartialChange.InitialRetryLoadChapterPartialChange.Data(fromDomain(it)) }
        )
        .let { emit(it) }
    }
  }.flowOn(dispatcherProvider.ui).asObservable()

  override fun refresh(chapter: ViewState.Chapter, isDownloaded: Boolean) = flow<PartialChange> {
    Timber.tag("LoadChapter###").d("refresh ${chapter.debug}")

    emit(PartialChange.RefreshPartialChange.Loading)

    if (isDownloaded) {
      var isFirstEvent = true
      downloadComicsRepository
        .getDownloadedChapter(chapter.link)
        .map {
          if (isFirstEvent) {
            it.fold(
              left = { PartialChange.RefreshPartialChange.Error(it) },
              right = { PartialChange.RefreshPartialChange.Success(fromDomain(it)) }
            ).also { isFirstEvent = false }
          } else {
            val initial = ViewState.Detail.Initial(chapter)
            it.fold(
              left = { PartialChange.InitialRetryLoadChapterPartialChange.Error(it, TODO()) },
              right = { PartialChange.InitialRetryLoadChapterPartialChange.Data(fromDomain(it)) }
            )
          }
        }
        .let { emitAll(it) }
    } else {
      comicRepository
        .getChapterDetail(chapter.link)
        .fold(
          left = { PartialChange.RefreshPartialChange.Error(it) },
          right = { PartialChange.RefreshPartialChange.Success(fromDomain(it)) }
        )
        .let { emit(it) }
    }
  }.flowOn(dispatcherProvider.ui).asObservable()
}