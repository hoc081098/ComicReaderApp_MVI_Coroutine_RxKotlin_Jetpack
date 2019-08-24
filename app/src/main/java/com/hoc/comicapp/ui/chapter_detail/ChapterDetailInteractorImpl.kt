package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatcherProvider
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.Initial_Retry_LoadChapter_PartialChange
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.RefreshPartialChange
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.rx2.asObservable

@ExperimentalCoroutinesApi
class ChapterDetailInteractorImpl(
  private val comicRepository: ComicRepository,
  private val dispatcherProvider: CoroutinesDispatcherProvider
) : ChapterDetailInteractor {
  override fun getChapterDetail(
    chapterLink: String,
    chapterName: String?
  ): Observable<Initial_Retry_LoadChapter_PartialChange> {
    return flow {
      if (chapterName != null) {
        val initial = ChapterDetailViewState.Detail.Initial(
          chapterLink = chapterLink,
          chapterName = chapterName
        )
        emit(Initial_Retry_LoadChapter_PartialChange.InitialData(initial))
      }

      emit(Initial_Retry_LoadChapter_PartialChange.Loading)

      emit(
        comicRepository
          .getChapterDetail(chapterLink)
          .fold(
            left = { Initial_Retry_LoadChapter_PartialChange.Error(it) },
            right = { Initial_Retry_LoadChapter_PartialChange.Data(it) }
          )
      )
    }.flowOn(dispatcherProvider.ui).asObservable()
  }

  override fun refresh(chapterLink: String): Observable<RefreshPartialChange> {
    return flow {
      emit(RefreshPartialChange.Loading)

      emit(
        comicRepository
          .getChapterDetail(chapterLink)
          .fold(
            left = { RefreshPartialChange.Error(it) },
            right = { RefreshPartialChange.Success(it) }
          )
      )
    }.flowOn(dispatcherProvider.ui).asObservable()
  }
}