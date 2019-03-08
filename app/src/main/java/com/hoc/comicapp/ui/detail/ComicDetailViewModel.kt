package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.base.BaseViewModel
import com.hoc.comicapp.utils.notOfType
import com.hoc.comicapp.utils.setValueIfNew
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import timber.log.Timber

@ExperimentalCoroutinesApi
class ComicDetailViewModel(private val comicDetailInteractor: ComicDetailInteractor) :
  BaseViewModel<ComicDetailIntent, ComicDetailViewState, ComicDetailSingleEvent>() {
  override val initialState = ComicDetailViewState.initialState()

  private val compositeDisposable = CompositeDisposable()
  private val intentS = PublishRelay.create<ComicDetailIntent>()
  private val stateS = BehaviorRelay.createDefault<ComicDetailViewState>(initialState)

  override fun processIntents(intents: Observable<ComicDetailIntent>) =
    intents.subscribe(intentS::accept)!!

  private val initialProcessor =
    ObservableTransformer<ComicDetailIntent.Initial, ComicDetailPartialChange> { intent ->
      intent.flatMap { comicDetailInteractor.getComicDetail(scope, it.link, it.name, it.thumbnail) }
    }

  private val refreshProcessor =
    ObservableTransformer<ComicDetailIntent.Refresh, ComicDetailPartialChange> {
      Observable.empty()
    }

  private val intentToViewState = ObservableTransformer<ComicDetailIntent, ComicDetailViewState> {
    it.publish { shared ->
      Observable.mergeArray(
        shared.ofType<ComicDetailIntent.Initial>().compose(initialProcessor),
        shared.ofType<ComicDetailIntent.Refresh>().compose(refreshProcessor)
      )
    }.doOnNext { Timber.d("partial_change=$it") }
      .scan(ComicDetailViewState.initialState()) { state, change -> change.reducer(state) }
      .distinctUntilChanged()
      .observeOn(AndroidSchedulers.mainThread())
  }

  init {
    intentS
      .compose(intentFilter)
      .doOnNext { Timber.d("intent=$it") }
      .compose(intentToViewState)
      .doOnNext { Timber.d("view_state=$it") }
      .subscribeBy(onNext = stateS::accept)
      .addTo(compositeDisposable)

    stateS
      .subscribeBy(onNext = stateD::setValueIfNew)
      .addTo(compositeDisposable)
  }


  private companion object {
    private val intentFilter = ObservableTransformer<ComicDetailIntent, ComicDetailIntent> {
      it.publish { shared ->
        Observable.mergeArray(
          shared.ofType<ComicDetailIntent.Initial>().take(1),
          shared.notOfType<ComicDetailIntent.Initial, ComicDetailIntent>()
        )
      }
    }
  }
}