package com.hoc.comicapp.ui.category_detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.*
import com.hoc.comicapp.utils.notOfType
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.Observable.mergeArray
import io.reactivex.ObservableTransformer
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy

class CategoryDetailVM(
  private val rxSchedulerProvider: RxSchedulerProvider,
  private val interactor: Interactor
) : BaseViewModel<ViewIntent, ViewState, SingleEvent>() {
  override val initialState = ViewState.initial()
  private val intentS = PublishRelay.create<ViewIntent>()

  override fun processIntents(intents: Observable<ViewIntent>) = intents.subscribe(intentS)!!

  private val initialProcessor = ObservableTransformer<ViewIntent.Initial, PartialChange> { intent ->
    intent.flatMap { (arg) ->
      mergeArray(
        interactor.getPopulars(categoryLink = arg.link),
        interactor.getComics(categoryLink = arg.link, page = 1)
      )
    }
  }

  private val intentToChanges = ObservableTransformer<ViewIntent, PartialChange> { intent ->
    intent.publish { shared ->
      mergeArray(
        shared.ofType<ViewIntent.Initial>().compose(initialProcessor)
      )
    }
  }

  init {
    intentS
      .compose(intentFilter)
      .compose(intentToChanges)
      .scan(initialState) { vs, change -> change.reducer(vs) }
      .observeOn(rxSchedulerProvider.main)
      .subscribeBy(onNext = ::setNewState)
      .addTo(compositeDisposable)
  }

  private companion object {
    val intentFilter = ObservableTransformer<ViewIntent, ViewIntent> { intent ->
      intent.publish { shared ->
        mergeArray(
          shared.ofType<ViewIntent.Initial>().take(1),
          shared.notOfType<ViewIntent.Initial, ViewIntent>()
        )
      }
    }
  }
}