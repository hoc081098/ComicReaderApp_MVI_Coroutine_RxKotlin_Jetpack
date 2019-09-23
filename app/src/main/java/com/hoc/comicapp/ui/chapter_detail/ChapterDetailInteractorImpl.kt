package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.InitialRetryLoadChapterPartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.RefreshPartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Chapter
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewState.Detail.Companion.fromDomain
import com.hoc.comicapp.utils.fold
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.rx2.asObservable
import timber.log.Timber

@ExperimentalCoroutinesApi
class ChapterDetailInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : ChapterDetailInteractor {
  override fun getChapterDetail(chapter: Chapter) = flow {
    Timber.tag("LoadChapter###").d("getChapterDetail ${chapter.debug}")

    val initial = ChapterDetailViewState.Detail.Initial(chapter)
    emit(InitialRetryLoadChapterPartialChange.InitialData(initial))

    emit(InitialRetryLoadChapterPartialChange.Loading)

    emit(
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
    )
  }.flowOn(dispatcherProvider.ui).asObservable()

  override fun refresh(chapter: Chapter) = flow {
    Timber.tag("LoadChapter###").d("refresh ${chapter.debug}")

    emit(RefreshPartialChange.Loading)

    emit(
      comicRepository
        .getChapterDetail(chapter.link)
        .fold(
          left = { RefreshPartialChange.Error(it) },
          right = { RefreshPartialChange.Success(fromDomain(it)) }
        )
    )
  }.flowOn(dispatcherProvider.ui).asObservable()
}