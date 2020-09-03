package com.hoc.comicapp.data.repository

import com.hoc.comicapp.data.ErrorMapper
import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicAppError
import com.hoc.comicapp.domain.repository.ComicRepository
import com.hoc.comicapp.utils.Left
import com.hoc.comicapp.utils.getOrThrow
import com.hoc.comicapp.utils.right
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import kotlinx.coroutines.delay
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.rx3.rxSingle
import timber.log.Timber

@Suppress("unused")
class ComicRepository1Impl(
  private val comicRepositoryImpl: ComicRepositoryImpl,
  private val errorMapper: ErrorMapper,
) :
  ComicRepository by comicRepositoryImpl {
  override suspend fun refreshAll(): DomainResult<Triple<List<Comic>, List<Comic>, List<Comic>>> {
    return Singles
      .zipDomainResult(
        rxSingle { getNewestComics() },
        rxSingle { getMostViewedComics() },
        rxSingle { getUpdatedComics() },
        errorMapper
      )
      .await()
      .also { result ->
        if (result is Left<ComicAppError>) {
          result.value.let { Timber.d(it, "ComicRepositoryImpl1::refreshAll [ERROR] $it") }
          delay(500)
        }
      }
  }
}

fun <T1 : Any, T2 : Any, T3 : Any> Singles.zipDomainResult(
  source1: Single<DomainResult<T1>>,
  source2: Single<DomainResult<T2>>,
  source3: Single<DomainResult<T3>>,
  errorMapper: ErrorMapper,
): Single<DomainResult<Triple<T1, T2, T3>>> {
  return zip(
    source1.map { it.getOrThrow() },
    source2.map { it.getOrThrow() },
    source3.map { it.getOrThrow() }
  )
    .map { (t1, t2, t3) ->
      @Suppress("USELESS_CAST")
      Triple(t1, t2, t3).right() as DomainResult<Triple<T1, T2, T3>>
    }
    .onErrorReturn { errorMapper.mapAsLeft(it) }
}
