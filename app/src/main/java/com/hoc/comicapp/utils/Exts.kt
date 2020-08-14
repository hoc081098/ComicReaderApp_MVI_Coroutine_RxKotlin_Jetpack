@file:Suppress("SpellCheckingInspection", "unused")

package com.hoc.comicapp.utils

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.net.Uri
import android.os.Looper
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.Toast
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jakewharton.rxbinding4.InitialValueObservable
import com.jakewharton.rxrelay3.Relay
import com.jaredrummler.materialspinner.MaterialSpinner
import com.miguelcatalan.materialsearchview.MaterialSearchView
import io.reactivex.rxjava3.android.MainThreadDisposable
import io.reactivex.rxjava3.android.MainThreadDisposable.verifyMainThread
import io.reactivex.rxjava3.annotations.SchedulerSupport
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.ofType
import io.reactivex.rxjava3.subjects.Subject
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.roundToInt
import androidx.lifecycle.Observer as LiveDataObserver

@CheckResult
@SchedulerSupport(SchedulerSupport.NONE)
inline fun <reified U : Any, T : Any> Observable<T>.notOfType() = filter { it !is U }!!

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
inline fun <T : Any, R : Any> Observable<T>.mapNotNull(crossinline transform: (T) -> R?): Observable<R> {
  return map { transform(it).toOptional() }
    .ofType<Some<R>>()
    .map { it.value }
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

/**
 * Get uri from any resource type
 * @receiver Context
 * @param resId - Resource id
 * @return - Uri to resource by given id or null
 */
fun Context.uriFromResourceId(@AnyRes resId: Int): Uri? {
  return runCatching {
    val res = this@uriFromResourceId.resources
    Uri.parse(
      ContentResolver.SCHEME_ANDROID_RESOURCE +
          "://" + res.getResourcePackageName(resId)
          + '/' + res.getResourceTypeName(resId)
          + '/' + res.getResourceEntryName(resId)
    )
  }.getOrNull()
}

fun Context.dpToPx(dp: Int): Int {
  val displayMetrics = resources.displayMetrics
  return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
}

@Suppress("nothing_to_inline")
inline fun Context.toast(
  @StringRes messageRes: Int,
  short: Boolean = true,
) = this.toast(getString(messageRes), short)

@Suppress("nothing_to_inline")
inline fun Context.toast(
  message: String,
  short: Boolean = true,
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

  abstract val length: Int
}

@SuppressLint("Recycle")
fun Context.themeInterpolator(@AttrRes attr: Int): Interpolator {
  return AnimationUtils.loadInterpolator(
    this,
    obtainStyledAttributes(intArrayOf(attr)).use {
      it.getResourceId(0, android.R.interpolator.fast_out_slow_in)
    }
  )
}


inline fun View.snack(
  @StringRes messageRes: Int,
  length: SnackbarLength = SnackbarLength.SHORT,
  crossinline f: Snackbar.() -> Unit = {},
) = snack(resources.getString(messageRes), length, f)

inline fun View.snack(
  message: String,
  length: SnackbarLength = SnackbarLength.SHORT,
  crossinline f: Snackbar.() -> Unit = {},
) = Snackbar.make(this, message, length.length).apply {
  f()
  show()
}

fun Snackbar.action(
  @StringRes actionRes: Int,
  color: Int? = null,
  listener: (View) -> Unit,
) = action(view.resources.getString(actionRes), color, listener)

fun Snackbar.action(
  action: String,
  color: Int? = null,
  listener: (View) -> Unit,
) = apply {
  setAction(action, listener)
  color?.let { setActionTextColor(color) }
}


fun Snackbar.onDismissed(f: () -> Unit) {
  addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar?>() {
    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
      super.onDismissed(transientBottomBar, event)
      f()
      removeCallback(this)
    }
  })
}

inline fun <T : Any> NotNullLiveData<T>.observe(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit,
) = Observer { value: T -> observer(value) }
  .also { observe(owner, it) }

fun <T : Any> LiveData<T>.toObservable(fallbackNullValue: (() -> T)? = null): Observable<T> {
  return Observable.create { emitter: ObservableEmitter<T> ->
    verifyMainThread()

    val observer = LiveDataObserver<T> { value: T? ->
      if (!emitter.isDisposed) {
        val notnullValue: T = value ?: fallbackNullValue?.invoke() ?: return@LiveDataObserver
        emitter.onNext(notnullValue)
      }
    }
    observeForever(observer)

    emitter.setDisposable(object : MainThreadDisposable() {
      override fun onDispose() {
        removeObserver(observer)
      }
    })
  }
}

inline fun <T : Any> LiveData<Event<T>>.observeEvent(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit,
) = Observer { event: Event<T>? ->
  event?.getContentIfNotHandled()?.let(observer)
}.also { observe(owner, it) }

typealias RxObserver<T> = io.reactivex.rxjava3.core.Observer<T>

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


fun InputStream.copyTo(
  target: File,
  overwrite: Boolean = false,
  bufferSize: Int = DEFAULT_BUFFER_SIZE,
): File {
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

suspend fun <T> retryIO(
  times: Int,
  initialDelay: Long,
  factor: Double,
  maxDelay: Long = Long.MAX_VALUE,
  block: suspend () -> T,
): T {
  var currentDelay = initialDelay
  repeat(times - 1) {
    try {
      return block()
    } catch (e: IOException) {
      // you can log an error here and/or make a more finer-grained
      // analysis of the cause to see if retry is needed
    }
    delay(currentDelay)
    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
  }
  return block() // last attempt
}

fun DocumentReference.snapshots(): Observable<DocumentSnapshot> {
  return Observable.create { emitter: ObservableEmitter<DocumentSnapshot> ->
    val registration = addSnapshotListener listener@{ documentSnapshot, exception ->
      if (exception !== null && !emitter.isDisposed) {
        return@listener emitter.onError(exception)
      }
      if (documentSnapshot != null && !emitter.isDisposed) {
        emitter.onNext(documentSnapshot)
      }
    }
    emitter.setCancellable {
      registration.remove()
      Timber.d("Remove snapshot listener $this")
    }
  }
}

fun Query.snapshots(): Observable<QuerySnapshot> {
  return Observable.create { emitter: ObservableEmitter<QuerySnapshot> ->
    val registration = addSnapshotListener listener@{ querySnapshot, exception ->
      if (exception !== null && !emitter.isDisposed) {
        return@listener emitter.onError(exception)
      }
      if (querySnapshot != null && !emitter.isDisposed) {
        emitter.onNext(querySnapshot)
      }
    }
    emitter.setCancellable {
      registration.remove()
      Timber.d("Remove snapshot listener $this")
    }
  }
}

@Suppress("unused")
inline val Any?.unit
  get() = Unit

inline val ViewGroup.inflater: LayoutInflater get() = LayoutInflater.from(context)
