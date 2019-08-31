package com.hoc.comicapp.ui.downloaded_comics

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.domain.models.ComicAppError
import io.reactivex.Observable
import com.hoc.comicapp.base.SingleEvent as BaseSingleEvent
import com.hoc.comicapp.base.ViewState as BaseViewState

interface DownloadedComicsContract {
  interface Interactor {
    fun getDownloadedComics(): Observable<PartialChange>
  }

  sealed class ViewIntent : Intent {
    object Initial : ViewIntent()
  }

  data class ViewState(
    val isLoading: Boolean,
    val error: ComicAppError?,
    val comics: List<ComicItem>
  ) : BaseViewState {

    companion object {
      fun initial(): ViewState {
        return ViewState(
          isLoading = true,
          error = null,
          comics = emptyList()
        )
      }
    }

    data class ComicItem(
      val title: String
    )
  }

  sealed class PartialChange {
    data class Data(val comics: List<ViewState.ComicItem>) : PartialChange()

    data class Error(val error: ComicAppError) : PartialChange()

    object Loading : PartialChange()
  }

  sealed class SingleEvent : BaseSingleEvent {
    data class Message(val message: String) : SingleEvent()
  }
}