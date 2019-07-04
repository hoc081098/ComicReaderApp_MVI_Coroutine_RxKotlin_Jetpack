package com.hoc.comicapp.ui.chapter_detail

import com.hoc.comicapp.CoroutinesDispatcherProvider
import com.hoc.comicapp.domain.ComicRepository
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailPartialChange.InitialRetryPartialChange
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
) :
  ChapterDetailInteractor {
  override fun getChapterDetail(
    chapterLink: String,
    chapterName: String?,
    time: String?,
    view: String?
  ): Observable<InitialRetryPartialChange> {
    return flow {
      if (chapterName != null && time != null && view != null) {
        val initial = ChapterDetailViewState.Detail.Initial(
          chapterLink = chapterLink,
          view = view,
          time = time,
          chapterName = chapterName
        )
        emit(InitialRetryPartialChange.InitialData(initial))
      }

      emit(InitialRetryPartialChange.Loading)

      emit(
        comicRepository
          .getChapterDetail(chapterLink)
          .fold(
            left = { InitialRetryPartialChange.Error(it) },
            right = { InitialRetryPartialChange.Data(it) }
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