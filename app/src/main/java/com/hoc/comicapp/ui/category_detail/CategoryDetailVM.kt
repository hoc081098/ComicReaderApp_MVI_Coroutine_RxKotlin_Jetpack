package com.hoc.comicapp.ui.category_detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.*
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable

class CategoryDetailVM : BaseViewModel<ViewIntent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()
  private val intentS = PublishRelay.create<ViewIntent>()

  override fun processIntents(intents: Observable<ViewIntent>) = intents.subscribe(intentS)!!

  init {

  }
}