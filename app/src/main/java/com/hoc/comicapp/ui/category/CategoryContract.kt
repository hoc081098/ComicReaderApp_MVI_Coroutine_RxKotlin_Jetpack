package com.hoc.comicapp.ui.category

import androidx.annotation.StringDef
import com.hoc.comicapp.base.Intent
import com.hoc.comicapp.base.SingleEvent
import com.hoc.comicapp.base.ViewState
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.models.getMessage
import io.reactivex.Observable

interface CategoryInteractor {
  fun getAllCategories(): Observable<CategoryPartialChange.InitialRetryPartialChange>

  fun refresh(): Observable<CategoryPartialChange.RefreshPartialChange>
}

data class CategoryViewState(
  val isLoading: Boolean,
  val categories: List<Category>,
  val errorMessage: String?,
  val refreshLoading: Boolean,
  @SortOrder val sortOrder: String,
) : ViewState {
  companion object {
    @JvmStatic
    fun initialState() = CategoryViewState(
      isLoading = false,
      categories = emptyList(),
      errorMessage = null,
      refreshLoading = false,
      sortOrder = CATEGORY_NAME_ASC
    )
  }
}

@StringDef(value = [CATEGORY_NAME_ASC, CATEGORY_NAME_DESC])
@Retention(value = AnnotationRetention.SOURCE)
private annotation class SortOrder

const val CATEGORY_NAME_ASC = "Name ascending"
const val CATEGORY_NAME_DESC = "Name descending"

sealed class CategoryPartialChange {
  abstract fun reducer(state: CategoryViewState): CategoryViewState

  sealed class InitialRetryPartialChange : CategoryPartialChange() {
    override fun reducer(state: CategoryViewState): CategoryViewState {
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

    data class Data(val categories: List<Category>) : InitialRetryPartialChange()
    object Loading : InitialRetryPartialChange()
    data class Error(val error: ComicAppError) : InitialRetryPartialChange()
  }

  sealed class RefreshPartialChange : CategoryPartialChange() {
    override fun reducer(state: CategoryViewState): CategoryViewState {
      return when (this) {
        Loading -> state.copy(refreshLoading = true)
        is Data -> state.copy(
          refreshLoading = false,
          errorMessage = null,
          categories = categories
        )
        is Error -> state.copy(refreshLoading = false)
      }
    }

    data class Data(val categories: List<Category>) : RefreshPartialChange()
    object Loading : RefreshPartialChange()
    data class Error(val error: ComicAppError) : RefreshPartialChange()
  }
}

sealed class CategoryViewIntent : Intent {
  object Initial : CategoryViewIntent()
  object Refresh : CategoryViewIntent()
  object Retry : CategoryViewIntent()
  data class ChangeSortOrder(@SortOrder val sortOrder: String) : CategoryViewIntent()
}

sealed class CategorySingleEvent : SingleEvent {
  data class MessageEvent(val message: String) : CategorySingleEvent()
}