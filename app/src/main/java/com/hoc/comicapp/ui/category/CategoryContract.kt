package com.hoc.comicapp.ui.category

import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope

interface CategoryInteractor {
  fun getAllCategories(coroutineScope: CoroutineScope): Observable<CategoryPartialChange>
}

data class CategoryViewState(
  val isLoading: Boolean,
  val categories: List<Category>,
  val errorMessage: String?
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState() = CategoryViewState(
      isLoading = false,
      categories = emptyList(),
      errorMessage = null
    )
  }
}

sealed class CategoryPartialChange {
  fun reducer(state: CategoryViewState): CategoryViewState {
    return when (this) {
      is Data -> {
        state.copy(
          isLoading = false,
          errorMessage = null,
          categories = categories
        )
      }
      Loading -> {
        state.copy(
          isLoading = true,
          errorMessage = null
        )
      }
      is Error -> {
        state.copy(
          isLoading = false,
          errorMessage = "Error occurred: ${error.getMessage()}"
        )
      }
    }
  }

  data class Data(val categories: List<Category>) : CategoryPartialChange()
  object Loading : CategoryPartialChange()
  data class Error(val error: ComicAppError) : CategoryPartialChange()
}

sealed class CategoryViewIntent : Intent {
  object Initial : CategoryViewIntent()
  object Refresh : CategoryViewIntent()
  object Retry : CategoryViewIntent()
}

sealed class CategorySingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : CategorySingleEvent()
}