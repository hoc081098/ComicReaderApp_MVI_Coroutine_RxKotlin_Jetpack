package com.hoc.comicapp.utils

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.InitialValueObservable
import com.jakewharton.rxrelay2.Relay
import com.jaredrummler.materialspinner.MaterialSpinner
import com.miguelcatalan.materialsearchview.MaterialSearchView
import com.shopify.livedataktx.LiveDataKtx
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.android.MainThreadDisposable
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.SchedulerSupport
import io.reactivex.disposables.Disposables
import io.reactivex.subjects.Subject
import java.io.File
import java.io.InputStream
import androidx.lifecycle.Observer as LiveDataObserver

@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
inline fun <reified U : Any, T : Any> Observable<T>.notOfType() = filter { it !is U }!!

@Suppress("nothing_to_inline")
inline fun <T> Relay<T>.asObservable(): Observable<T> = this

@Suppress("nothing_to_inline")
inline fun <T> Subject<T>.asObservable(): Observable<T> = this

inline fun <T, R> Observable<T>.exhaustMap(crossinline transform: (T) -> Observable<R>): Observable<R> {
  return this
    .toFlowable(BackpressureStrategy.DROP)
    .flatMap({ transform(it).toFlowable(BackpressureStrategy.MISSING) }, 1)
    .toObservable()
}

@Suppress("nothing_to_inline")
inline infix fun ViewGroup.inflate(layoutRes: Int) =
  LayoutInflater.from(context).inflate(layoutRes, this, false)!!

val Context.isOrientationPortrait get() = this.resources.configuration.orientation == ORIENTATION_PORTRAIT

@Suppress("nothing_to_inline")
@ColorInt
inline fun Context.getColorBy(@ColorRes id: Int) = ContextCompat.getColor(this, id)

@Suppress("nothing_to_inline")
inline fun Context.getDrawableBy(@DrawableRes id: Int) = ContextCompat.getDrawable(this, id)

@Suppress("nothing_to_inline")
inline fun Context.toast(
  @StringRes messageRes: Int,
  short: Boolean = true
) = this.toast(getString(messageRes), short)

@Suppress("nothing_to_inline")
inline fun Context.toast(
  message: String,
  short: Boolean = true
) =
  Toast.makeText(
    this,
    message,
    if (short) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
  ).apply { show() }!!


enum class SnackbarLength {
  SHORT {
    override val length = Snackbar.LENGTH_SHORT
  },
  LONG {
    override val length = Snackbar.LENGTH_LONG
  },
  INDEFINITE {
    override val length = Snackbar.LENGTH_INDEFINITE
  };

  @Snackbar.Duration
  abstract val length: Int
}


inline fun View.snack(
  @StringRes messageRes: Int,
  length: SnackbarLength = SnackbarLength.SHORT,
  crossinline f: Snackbar.() -> Unit = {}
) = snack(resources.getString(messageRes), length, f)

inline fun View.snack(
  message: String,
  length: SnackbarLength = SnackbarLength.SHORT,
  crossinline f: Snackbar.() -> Unit = {}
) = Snackbar.make(this, message, length.length).apply {
  f()
  show()
}

fun Snackbar.action(
  @StringRes actionRes: Int,
  color: Int? = null,
  listener: (View) -> Unit
) = action(view.resources.getString(actionRes), color, listener)

fun Snackbar.action(
  action: String,
  color: Int? = null,
  listener: (View) -> Unit
) = apply {
  setAction(action, listener)
  color?.let { setActionTextColor(color) }
}

inline fun <T> LiveDataKtx<T>.observe(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit
) = Observer<T?> { it?.let { observer(it) } }.also { observe(owner, it) }

fun <T> LiveData<T>.toObservable(fallbackNullValue: (() -> T)? = null): Observable<T> {
  return Observable.create { emitter: ObservableEmitter<T> ->
    val observer = LiveDataObserver<T> { value: T? ->
      if (!emitter.isDisposed) {
        val notnullValue = value ?: fallbackNullValue?.invoke() ?: return@LiveDataObserver
        emitter.onNext(notnullValue)
      }
    }
    observeForever(observer)
    emitter.setCancellable { removeObserver(observer) }
  }
}

inline fun <T> LiveData<Event<T>>.observeEvent(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit
) = Observer { event: Event<T>? ->
  event?.getContentIfNotHandled()?.let(observer)
}.also { observe(owner, it) }

typealias RxObserver<T> = io.reactivex.Observer<T>

private fun checkMainThread(observer: RxObserver<*>): Boolean {
  if (Looper.myLooper() != Looper.getMainLooper()) {
    observer.onSubscribe(Disposables.empty())
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
    private val observer: RxObserver<in T>
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
    private val observer: RxObserver<in String>
  ) : MainThreadDisposable(), MaterialSearchView.OnQueryTextListener {
    override fun onQueryTextChange(newText: String?): Boolean {
      return newText?.let {
        if (!isDisposed) {
          observer.onNext(it)
        }
        true
      } == true
    }

    override fun onQueryTextSubmit(query: String?) = true

    override fun onDispose() = view.setOnQueryTextListener(null)
  }
}


fun InputStream.copyTo(target: File, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): File {
  if (target.exists()) {
    val stillExists = if (!overwrite) true else !target.delete()

    if (stillExists) {
      throw IllegalAccessException("The destination file already exists.")
    }
  }

  target.parentFile?.mkdirs()

  this.use { input ->
    target.outputStream().use { output ->
      input.copyTo(output, bufferSize)
    }
  }

  return target
}

fun <A, B, C, R> LiveData<A>.combineLatest(b: LiveData<B>, c: LiveData<C>, combine: (A, B, C) -> R): LiveData<R> {
  return MediatorLiveData<R>().apply {
    var lastA: A? = null
    var lastB: B? = null
    var lastC: C? = null

    addSource(this@combineLatest) {
      if (it == null && value != null) value = null
      lastA = it

      lastA?.let { a ->
        lastB?.let { b ->
          lastC?.let { value = combine(a, b, it) }
        }
      }
    }

    addSource(b) {
      if (it == null && value != null) value = null
      lastB = it

      lastA?.let { a ->
        lastB?.let { b ->
          lastC?.let { value = combine(a, b, it) }
        }
      }
    }

    addSource(c) {
      if (it == null && value != null) value = null
      lastC = it

      lastA?.let { a ->
        lastB?.let { b ->
          lastC?.let { value = combine(a, b, it) }
        }
      }
    }
  }
}