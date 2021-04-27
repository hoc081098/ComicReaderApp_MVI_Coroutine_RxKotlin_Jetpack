package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.repository.DownloadComicsRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.Interactor
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.PartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.PartialChange.GetChapterDetail
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.PartialChange.Refresh
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState.Detail.Companion.fromDomain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx3.asObservable
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterDetailInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
  private val downloadComicsRepository: DownloadComicsRepository,
) : Interactor {
  override fun getChapterDetail(chapter: ViewState.Chapter, isDownloaded: Boolean) =
    flow<PartialChange> {
      Timber.tag("LoadChapter###").d("getChapterDetail ${chapter.debug}")

      emit(GetChapterDetail.Initial(chapter))

      emit(GetChapterDetail.Loading)

      if (isDownloaded) {
        downloadComicsRepository
          .getDownloadedChapter(chapter.link)
          .map { either ->
            either.fold(
              ifLeft = { GetChapterDetail.Error(it, chapter) },
              ifRight = { GetChapterDetail.Data(fromDomain(it)) }
            )
          }
          .let { emitAll(it) }
      } else {
        comicRepository
          .getChapterDetail(chapter.link)
          .fold(
            ifLeft = { GetChapterDetail.Error(it, chapter) },
            ifRight = { GetChapterDetail.Data(fromDomain(it)) }
          )
          .let { emit(it) }
      }
    }.flowOn(dispatchersProvider.main).asObservable()

  override fun refresh(chapter: ViewState.Chapter, isDownloaded: Boolean) = flow {
    Timber.tag("LoadChapter###").d("refresh ${chapter.debug}")

    emit(Refresh.Loading)

    if (isDownloaded) {
      var isFirstEvent = true

      downloadComicsRepository
        .getDownloadedChapter(chapter.link)
        .map { either ->
          if (isFirstEvent) {
            either
              .fold(
                ifLeft = { Refresh.Error(it) },
                ifRight = { Refresh.Success(fromDomain(it)) }
              )
              .also { isFirstEvent = false }
          } else {
            either.fold(
              ifLeft = { GetChapterDetail.Error(it, chapter) },
              ifRight = { GetChapterDetail.Data(fromDomain(it)) }
            )
          }
        }
        .let { emitAll(it) }
    } else {
      comicRepository
        .getChapterDetail(chapter.link)
        .fold(
          ifLeft = { Refresh.Error(it) },
          ifRight = { Refresh.Success(fromDomain(it)) }
        )
        .let { emit(it) }
    }
  }.flowOn(dispatchersProvider.main).asObservable()
}
