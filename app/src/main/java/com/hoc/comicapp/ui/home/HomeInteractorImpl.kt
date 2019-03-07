package com.hoc.comicapp.ui.home

import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.utils.Left
import com.hoc.comicapp.utils.flatMap
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.getOrNull
import com.hoc.comicapp.utils.map
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.toObservable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class HomeInteractorImpl(private val comicRepository: ComicRepository) : HomeInteractor {
  override fun suggestComicsPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return coroutineScope.rxObservable<HomePartialChange> {
      /**
       * Send loading
       */
      this.send(HomePartialChange.SuggestHomePartialChange.Loading)

      /**
       * Get suggest list
       */
      val suggestResult = comicRepository.getSuggest()

      /**
       * Send success change
       */
      suggestResult
        .getOrNull()
        .orEmpty()
        .let { HomePartialChange.SuggestHomePartialChange.Data(it) }
        .let { this.send(it) }

      /**
       * Send error change
       */
      if (suggestResult is Left) {
        suggestResult
          .value
          .let { HomePartialChange.SuggestHomePartialChange.Error(it) }
          .let { this.send(it) }
      }
    }
  }

  override fun topMonthComicsPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return coroutineScope.rxObservable<HomePartialChange> {
      /**
       * Send loading
       */
      this.send(HomePartialChange.TopMonthHomePartialChange.Loading)

      /**
       * Get top month list
       */
      val topMonthResult = comicRepository.getTopMonth()

      /**
       * Send success change
       */
      topMonthResult
        .getOrNull()
        .orEmpty()
        .let { HomePartialChange.TopMonthHomePartialChange.Data(it) }
        .let { this.send(it) }

      /**
       * Send error change
       */
      if (topMonthResult is Left) {
        topMonthResult
          .value
          .let { HomePartialChange.TopMonthHomePartialChange.Error(it) }
          .let { this.send(it) }
      }
    }
  }

  override fun updatedComicsPartialChanges(
    coroutineScope: CoroutineScope,
    page: Int
  ): Observable<HomePartialChange> {
    return coroutineScope.rxObservable<HomePartialChange> {
      /**
       * Send loading
       */
      this.send(HomePartialChange.UpdatedPartialChange.Loading)

      /**
       * Get updated comics list
       */
      val topMonthResult = comicRepository.getUpdate(page = page)

      /**
       * Send success change
       */
      topMonthResult
        .getOrNull()
        .orEmpty()
        .let { HomePartialChange.UpdatedPartialChange.Data(it) }
        .let { this.send(it) }

      /**
       * Send error change
       */
      if (topMonthResult is Left) {
        topMonthResult
          .value
          .let { HomePartialChange.UpdatedPartialChange.Error(it) }
          .let { this.send(it) }
      }
    }
  }

  override fun refreshAllPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return Observables.zip(
      coroutineScope.rxObservable { send(comicRepository.getSuggest()) },
      coroutineScope.rxObservable { send(comicRepository.getTopMonth()) },
      coroutineScope.rxObservable { send(comicRepository.getUpdate()) }
    ).flatMap { (suggest, topMonth, updated) ->
      suggest.flatMap { suggestList ->
        topMonth.flatMap { topMonthList ->
          updated.map { updatedList ->
            listOf(
              HomePartialChange.TopMonthHomePartialChange.Data(topMonthList),
              HomePartialChange.SuggestHomePartialChange.Data(suggestList),
              HomePartialChange.UpdatedPartialChange.Data(updatedList),
              HomePartialChange.RefreshSuccess
            ).toObservable()
          }
        }
      }.fold({ Observable.just(HomePartialChange.RefreshFailure(it)) }, { it })
    }
  }
}