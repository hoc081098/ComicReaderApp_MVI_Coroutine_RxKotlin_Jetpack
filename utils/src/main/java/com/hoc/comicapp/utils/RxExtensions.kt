package com.hoc.comicapp.utils

import androidx.annotation.CheckResult
import com.jakewharton.rxrelay3.Relay
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.internal.disposables.DisposableHelper
import io.reactivex.rxjava3.internal.util.AtomicThrowable
import io.reactivex.rxjava3.subjects.Subject
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@CheckResult
@SchedulerSupport(SchedulerSupport.NONE)
inline fun <reified U : T, T : Any> Observable<T>.notOfType(): Observable<T> = filter { it !is U }

@Suppress("nothing_to_inline")
inline fun <T : Any> Relay<T>.asObservable(): Observable<T> = this

@Suppress("nothing_to_inline")
inline fun <T : Any> Subject<T>.asObservable(): Observable<T> = this

@CheckResult
fun <T : Any, R : Any> Observable<T>.exhaustMap(transform: (T) -> Observable<R>): Observable<R> =
  ExhaustMapObservable(this, transform)

class ExhaustMapObservable<T : Any, R : Any>(
  private val source: Observable<T>,
  private val transform: (T) -> Observable<out R>
) : Observable<R>() {
  override fun subscribeActual(observer: Observer<in R>) =
    source.subscribe(ExhaustMapObserver(observer, transform))

  private class ExhaustMapObserver<T : Any, R : Any>(
    private val downstream: Observer<in R>,
    private val transform: (T) -> Observable<out R>
  ) : Observer<T>, Disposable {
    private var upstream: Disposable? = null

    @Volatile
    private var isActive = false
    private val innerObserver = ExhaustMapInnerObserver()
    private val errors = AtomicThrowable()
    private val done = AtomicInteger(1)

    override fun onSubscribe(d: Disposable) {
      if (DisposableHelper.validate(upstream, d)) {
        upstream = d
        downstream.onSubscribe(this)
      }
    }

    override fun onNext(t: T) {
      if (isActive) {
        return
      }

      val o = try {
        transform(t)
      } catch (t: Throwable) {
        Exceptions.throwIfFatal(t)
        upstream!!.dispose()
        onError(t)
        return
      }

      isActive = true
      done.incrementAndGet()
      o.subscribe(innerObserver)
    }

    override fun onError(e: Throwable) {
      if (errors.tryAddThrowableOrReport(e)) {
        onComplete()
      }
    }

    override fun onComplete() {
      if (done.decrementAndGet() == 0) {
        errors.tryTerminateConsumer(downstream)
      }
    }

    override fun dispose() {
      upstream!!.dispose()
      DisposableHelper.dispose(innerObserver)
      errors.tryTerminateAndReport()
    }

    override fun isDisposed() = upstream!!.isDisposed

    private fun innerNext(t: R) {
      downstream.onNext(t)
    }

    private fun innerError(e: Throwable) {
      if (errors.tryAddThrowableOrReport(e)) {
        innerComplete()
      }
    }

    private fun innerComplete() {
      isActive = false
      if (done.decrementAndGet() == 0) {
        errors.tryTerminateConsumer(downstream)
      }
    }

    private inner class ExhaustMapInnerObserver : Observer<R>, AtomicReference<Disposable>() {
      override fun onSubscribe(d: Disposable) = DisposableHelper.replace(this, d).unit
      override fun onNext(t: R) = innerNext(t)
      override fun onError(e: Throwable) = innerError(e)
      override fun onComplete() = innerComplete()
    }
  }
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
    if (DisposableHelper.validate(upstream, d)) {
      upstream = d
      downstream.onSubscribe(this)
    }
  }

  override fun onNext(t: T) = transform(t)?.let(downstream::onNext).unit

  override fun onError(e: Throwable) = downstream.onError(e)

  override fun onComplete() = downstream.onComplete()

  override fun dispose() = upstream!!.dispose().unit

  override fun isDisposed(): Boolean = upstream!!.isDisposed
}
