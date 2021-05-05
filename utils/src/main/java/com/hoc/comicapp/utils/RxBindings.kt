package com.hoc.comicapp.utils

import android.os.Looper
import androidx.annotation.CheckResult
import com.jakewharton.rxbinding4.InitialValueObservable
import com.jaredrummler.materialspinner.MaterialSpinner
import com.miguelcatalan.materialsearchview.MaterialSearchView
import io.reactivex.rxjava3.android.MainThreadDisposable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable

typealias RxObserver<T> = Observer<T>

private fun checkMainThread(observer: RxObserver<*>): Boolean {
  if (Looper.myLooper() != Looper.getMainLooper()) {
    observer.onSubscribe(Disposable.empty())
    observer.onError(
      IllegalStateException(
        "Expected to be called on the main thread but was ${Thread.currentThread().name}"
      )
    )
    return false
  }
  return true
}

@CheckResult
fun MaterialSearchView.textChanges(): Observable<String> {
  return MaterialSearchViewObservable(this)
}

@CheckResult
fun <T : Any> MaterialSpinner.itemSelections(): InitialValueObservable<T> {
  return MaterialSpinnerSelectionObservable(this)
}

internal class MaterialSpinnerSelectionObservable<T : Any>(private val view: MaterialSpinner) :
  InitialValueObservable<T>() {
  override val initialValue get() = view.getItems<T>()[view.selectedIndex]!!

  override fun subscribeListener(observer: RxObserver<in T>) {
    if (!checkMainThread(observer)) {
      return
    }
    Listener(view, observer).let { listener ->
      view.setOnItemSelectedListener(listener)
      observer.onSubscribe(listener)
    }
  }

  private class Listener<T : Any>(
    private val view: MaterialSpinner,
    private val observer: RxObserver<in T>,
  ) : MaterialSpinner.OnItemSelectedListener<T>, MainThreadDisposable() {
    override fun onDispose() = view.setOnItemSelectedListener(null)

    override fun onItemSelected(view: MaterialSpinner?, position: Int, id: Long, item: T) {
      if (!isDisposed) {
        observer.onNext(item)
      }
    }
  }
}

internal class MaterialSearchViewObservable(private val view: MaterialSearchView) :
  Observable<String>() {
  override fun subscribeActual(observer: RxObserver<in String>) {
    if (!checkMainThread(observer)) {
      return
    }
    Listener(view, observer).let { listener ->
      observer.onSubscribe(listener)
      view.setOnQueryTextListener(listener)
    }
  }

  private class Listener(
    private val view: MaterialSearchView,
    private val observer: RxObserver<in String>,
  ) : MainThreadDisposable(), MaterialSearchView.OnQueryTextListener {
    override fun onQueryTextChange(newText: String?): Boolean {
      return newText?.let {
        if (!isDisposed) {
          observer.onNext(it)
        }
        true
      } == true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
      query?.let {
        if (!isDisposed) {
          observer.onNext(it)
        }
      }
      return false
    }

    override fun onDispose() = view.setOnQueryTextListener(null)
  }
}
