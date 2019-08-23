package com.hoc.comicapp.ui.home

import com.hoc.comicapp.domain.repository.ComicRepository
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
   * Newest list
   */
  override fun newestComics(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return coroutineScope.rxObservable {
      /**
       * Send loading
       */
      this.send(HomePartialChange.NewestHomePartialChange.Loading)

      /**
       * Get newest list
       */
      val newestResult = comicRepository.getNewestComics(null)

      /**
       * Send success change
       */
      newestResult
        .getOrNull()
        .orEmpty()
        .let { HomePartialChange.NewestHomePartialChange.Data(it) }
        .let { this.send(it) }

      /**
       * Send error change
       */
      if (newestResult is Left) {
        newestResult
          .value
          .let { HomePartialChange.NewestHomePartialChange.Error(it) }
          .let { this.send(it) }
      }
    }.cast()
  }

  /**
   * Most viewed list
   */
  override fun mostViewedComics(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return coroutineScope.rxObservable {
      /**
       * Send loading
       */
      this.send(HomePartialChange.MostViewedHomePartialChange.Loading)

      /**
       * Get most viewed list
       */
      val mostViewedResult = comicRepository.getMostViewedComics()

      /**
       * Send success change
       */
      mostViewedResult
        .getOrNull()
        .orEmpty()
        .let { HomePartialChange.MostViewedHomePartialChange.Data(it) }
        .let { this.send(it) }

      /**
       * Send error change
       */
      if (mostViewedResult is Left) {
        mostViewedResult
          .value
          .let { HomePartialChange.MostViewedHomePartialChange.Error(it) }
          .let { this.send(it) }
      }
    }.cast()
  }

  /**
   * Updated list
   */
  override fun updatedComics(
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
      val updatedResult = comicRepository.getUpdatedComics(page = page)


      /**
       * Send success change or error change
       */
      updatedResult.fold(
        { HomePartialChange.UpdatedPartialChange.Error(it) },
        { HomePartialChange.UpdatedPartialChange.Data(it) }
      ).let { send(it) }
    }.cast()
  }

  override fun refreshAll(coroutineScope: CoroutineScope): Observable<HomePartialChange> {
    return Observables.zip(
      coroutineScope.rxObservable { send(comicRepository.getNewestComics(null)) },
      coroutineScope.rxObservable { send(comicRepository.getMostViewedComics()) },
      coroutineScope.rxObservable { send(comicRepository.getUpdatedComics()) }
    ).map<HomePartialChange> { (newest, mostViewed, updated) ->
      newest.flatMap { newestList ->
        mostViewed.flatMap { mostViewedList ->
          updated.map { updatedList ->
            HomePartialChange.RefreshPartialChange.RefreshSuccess(
              newestComics = newestList,
              mostViewedComics = mostViewedList,
              updatedComics = updatedList
            )
          }
        }
      }.fold({ HomePartialChange.RefreshPartialChange.RefreshFailure(it) }, { it })
    }.startWith(HomePartialChange.RefreshPartialChange.Loading)
  }
}