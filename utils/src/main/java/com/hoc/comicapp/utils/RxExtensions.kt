package com.hoc.comicapp.utils

import androidx.annotation.CheckResult
import com.jakewharton.rxrelay3.Relay
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.Subject

@CheckResult
@SchedulerSupport(SchedulerSupport.NONE)
inline fun <reified U : T, T : Any> Observable<T>.notOfType(): Observable<T> = filter { it !is U }

@Suppress("nothing_to_inline")
inline fun <T : Any> Relay<T>.asObservable(): Observable<T> = this

@Suppress("nothing_to_inline")
inline fun <T : Any> Subject<T>.asObservable(): Observable<T> = this

@CheckResult
inline fun <T : Any, R : Any> Observable<T>.exhaustMap(crossinline transform: (T) -> Observable<R>): Observable<R> {
  return this
    .toFlowable(BackpressureStrategy.DROP)
    .flatMap({ transform(it).toFlowable(BackpressureStrategy.MISSING) }, 1)
    .toObservable()
}

@CheckResult
fun <T : Any, R : Any> Observable<T>.mapNotNull(transform: (T) -> R?): Observable<R> =
  lift { MapNotNullObserver(it, transform) }

private class MapNotNullObserver<T : Any, R : Any>(
  private val downstream: Observer<R>,
  private val transform: (T) -> R?
) : Observer<T>, Disposable {
  private var upstream: Disposable? = null

  override fun onSubscribe(d: Disposable) {
    if (upstream !== null) {
      d.dispose()
    } else {
      upstream = d
      downstream.onSubscribe(d)
    }
  }

  override fun onNext(t: T) = transform(t)?.let(downstream::onNext).unit

  override fun onError(e: Throwable) = downstream.onError(e)

  override fun onComplete() = downstream.onComplete()

  override fun dispose() = upstream!!.dispose().unit

  override fun isDisposed(): Boolean = upstream!!.isDisposed
}

