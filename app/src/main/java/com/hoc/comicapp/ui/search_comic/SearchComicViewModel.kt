package com.hoc.comicapp.ui.search_comic

import com.hoc.comicapp.base.BaseViewModel
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

class SearchComicViewModel : BaseViewModel<SearchComicViewIntent, SearchComicViewState, SearchComicSingleEvent>() {
  override val initialState = SearchComicViewState.initialState()

  override fun processIntents(intents: Observable<SearchComicViewIntent>): Disposable {
    TODO()
  }
}