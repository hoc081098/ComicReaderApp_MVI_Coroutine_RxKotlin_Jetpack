package com.hoc.comicapp.ui.home

import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.domain.thread.CoroutinesDispatchersProvider
import com.hoc.comicapp.ui.home.HomePartialChange.RefreshPartialChange.Loading
import com.hoc.comicapp.ui.home.HomePartialChange.RefreshPartialChange.RefreshFailure
import com.hoc.comicapp.ui.home.HomePartialChange.RefreshPartialChange.RefreshSuccess
import com.hoc.comicapp.utils.flatMap
import com.hoc.comicapp.utils.fold
import com.hoc.comicapp.utils.map
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class HomeInteractorImpl1(
  private val comicRepository: ComicRepository,
  private val homeInteractorImpl: HomeInteractorImpl,
  private val dispatchersProvider: CoroutinesDispatchersProvider,
) : HomeInteractor by homeInteractorImpl {
  override fun refreshAll(): Observable<HomePartialChange> {
    return rxObservable(dispatchersProvider.main) {
      coroutineScope {
        send(Loading)

        val newestAsync = async { comicRepository.getNewestComics() }
        val mostViewedAsync = async { comicRepository.getMostViewedComics() }
        val updatedAsync = async { comicRepository.getUpdatedComics() }

        val newest = newestAsync.await()
        val mostViewed = mostViewedAsync.await()
        val updated = updatedAsync.await()

        newest.flatMap { newestComics ->
            mostViewed.flatMap { mostViewedComics ->
              updated.map { updatedComics ->
                RefreshSuccess(
                  newestComics = newestComics,
                  mostViewedComics = mostViewedComics,
                  updatedComics = updatedComics
                )
              }
            }
          }
          .fold(
            { RefreshFailure(it) },
            { it }
          )
          .let { send(it) }
      }
    }
  }
}