package com.hoc.comicapp.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import io.reactivex.rxjava3.android.MainThreadDisposable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter

fun <T : Any> LiveData<T?>.toObservable(fallbackNullValue: (() -> T)? = null): Observable<T> {
  return Observable.create { emitter: ObservableEmitter<T> ->
    MainThreadDisposable.verifyMainThread()

    val observer = Observer<T?> { value: T? ->
      if (!emitter.isDisposed) {
        val notnullValue: T = value ?: fallbackNullValue?.invoke() ?: return@Observer
        emitter.onNext(notnullValue)
      }
    }
    observeForever(observer)

    emitter.setDisposable(
      object : MainThreadDisposable() {
        override fun onDispose() {
          removeObserver(observer)
        }
      }
    )
  }
}

inline fun <T : Any> LiveData<Event<T>>.observeEvent(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit,
) = Observer { event: Event<T>? ->
  event?.getContentIfNotHandled()?.let(observer)
}.also { observe(owner, it) }

fun <A, B, C, R> LiveData<A>.combineLatest(
  b: LiveData<B>,
  c: LiveData<C>,
  combine: (A, B, C) -> R,
): LiveData<R> {
  return MediatorLiveData<R>().apply {
    var lastA: A? = null
    var lastB: B? = null
    var lastC: C? = null

    addSource(this@combineLatest) { v ->
      if (v == null && value != null) value = null
      lastA = v

      lastA?.let { a ->
        lastB?.let { b ->
          lastC?.let { value = combine(a, b, it) }
        }
      }
    }

    addSource(b) { v ->
      if (v == null && value != null) value = null
      lastB = v

      lastA?.let { a ->
        lastB?.let { b ->
          lastC?.let { value = combine(a, b, it) }
        }
      }
    }

    addSource(c) { v ->
      if (v == null && value != null) value = null
      lastC = v

      lastA?.let { a ->
        lastB?.let { b ->
          lastC?.let { value = combine(a, b, it) }
        }
      }
    }
  }
}
