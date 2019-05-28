package com.hoc.comicapp.ui.home

import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.utils.*
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.cast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class HomeInteractorImpl(private val comicRepository: ComicRepository) : HomeInteractor {
  /**
   * Suggest list
   */
  override fun suggestComicsPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return coroutineScope.rxObservable {
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
    }.cast()
  }

  /**
   * Top month list
   */
  override fun topMonthComicsPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return coroutineScope.rxObservable {
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
    }.cast()
  }

  /**
   * Updated list
   */
  override fun updatedComicsPartialChanges(
    coroutineScope: CoroutineScope,
    page: Int
  ): Observable<HomePartialChange> {
    return coroutineScope.rxObservable {
      /**
       * Send loading
       */
      this.send(HomePartialChange.UpdatedPartialChange.Loading)

      /**
       * Get updated comics list
       */
      val updatedResult = comicRepository.getUpdate(page = page)


      /**
       * Send success change or error change
       */
      updatedResult.fold(
        { HomePartialChange.UpdatedPartialChange.Error(it) },
        { HomePartialChange.UpdatedPartialChange.Data(it) }
      ).let { send(it) }
    }.cast()
  }

  override fun refreshAllPartialChanges(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return Observables.zip(
      coroutineScope.rxObservable { send(comicRepository.getSuggest()) },
      coroutineScope.rxObservable { send(comicRepository.getTopMonth()) },
      coroutineScope.rxObservable { send(comicRepository.getUpdate()) }
    ).map<HomePartialChange> { (suggest, topMonth, updated) ->
      suggest.flatMap { suggestList ->
        topMonth.flatMap { topMonthList ->
          updated.map { updatedList ->
            HomePartialChange.RefreshPartialChange.RefreshSuccess(
              suggestComics = suggestList,
              topMonthComics = topMonthList,
              updatedComics = updatedList
            )
          }
        }
      }.fold({ HomePartialChange.RefreshPartialChange.RefreshFailure(it) }, { it })
    }.startWith(HomePartialChange.RefreshPartialChange.Loading)
  }
}