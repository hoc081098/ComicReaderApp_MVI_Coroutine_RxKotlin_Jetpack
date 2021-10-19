package com.hoc.comicapp.utils

import android.view.View
import androidx.annotation.StringRes
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

@JvmInline
value class SnackbarLength private constructor(val length: Int) {
  companion object Factory {
    val SHORT = SnackbarLength(Snackbar.LENGTH_SHORT)
    val LONG = SnackbarLength(Snackbar.LENGTH_LONG)
    val INDEFINITE = SnackbarLength(Snackbar.LENGTH_INDEFINITE)
  }
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
  addCallback(
    object : BaseTransientBottomBar.BaseCallback<Snackbar?>() {
      override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
        super.onDismissed(transientBottomBar, event)
        f()
        removeCallback(this)
      }
    }
  )
}
