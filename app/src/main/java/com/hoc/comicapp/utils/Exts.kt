package com.hoc.comicapp.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay2.Relay
import com.shopify.livedataktx.LiveDataKtx
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.annotations.CheckReturnValue
import io.reactivex.annotations.SchedulerSupport


@CheckReturnValue
@SchedulerSupport(SchedulerSupport.NONE)
inline fun <reified U : Any, T : Any> Observable<T>.notOfType() = filter { it !is U }!!

@Suppress("nothing_to_inline")
inline fun <T> Relay<T>.asObservable(): Observable<T> = this

inline fun <T, R> Observable<T>.exhaustMap(crossinline transform: (T) -> Observable<R>): Observable<R> {
  return this
    .toFlowable(BackpressureStrategy.DROP)
    .flatMap({ transform(it).toFlowable(BackpressureStrategy.MISSING) }, 1)
    .toObservable()
}


infix fun ViewGroup.inflate(layoutRes: Int) =
  LayoutInflater.from(context).inflate(layoutRes, this, false)!!

fun Context.toast(
  @StringRes messageRes: Int,
  short: Boolean = true
) = this.toast(getString(messageRes), short)

fun Context.toast(
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


inline fun <T> LiveData<Event<T>>.observeEvent(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit
) = Observer { event: Event<T>? ->
  event?.getContentIfNotHandled()?.let(observer)
}.also { observe(owner, it) }
