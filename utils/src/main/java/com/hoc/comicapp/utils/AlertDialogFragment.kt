package com.hoc.comicapp.utils

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import io.reactivex.rxjava3.android.MainThreadDisposable
import io.reactivex.rxjava3.android.MainThreadDisposable.verifyMainThread
import io.reactivex.rxjava3.core.Maybe
import timber.log.Timber

/**
 * Show alert dialog fragment
 * @return a [Maybe] that emits [Unit] when pressing OK button,
 * otherwise return an empty [Maybe]
 */
fun FragmentActivity.showAlertDialogAsMaybe(
  negativeText: String = "Cancel",
  positiveText: String = "OK",
  init: AlertDialogFragment.Builder.() -> Unit,
): Maybe<Unit> {
  return Maybe.create { emitter ->
    verifyMainThread()

    showAlertDialog {
      init()

      onCancel { emitter.onComplete() }

      negativeAction(negativeText) { _, _ -> emitter.onComplete() }

      positiveAction(positiveText) { _, _ ->
        emitter.onSuccess(Unit)
        emitter.onComplete()
      }
    }

    emitter.setDisposable(
      object : MainThreadDisposable() {
        override fun onDispose() = dismissAlertDialog()
      }
    )
  }
}

/**
 * Show alert dialog
 */
fun FragmentActivity.showAlertDialog(init: AlertDialogFragment.Builder.() -> Unit): AlertDialogFragment {
  val ft = supportFragmentManager.beginTransaction().apply {
    supportFragmentManager
      .findFragmentByTag(AlertDialogFragment::class.java.simpleName)
      ?.let(::remove)
    addToBackStack(null)
  }

  return AlertDialogFragment.Builder()
    .apply(init)
    .build()
    .apply { show(ft, AlertDialogFragment::class.java.simpleName) }
}

/**
 * Dismiss alert dialog
 */
fun FragmentActivity.dismissAlertDialog() {
  try {
    val dialogFragment =
      supportFragmentManager.findFragmentByTag(AlertDialogFragment::class.java.simpleName) as? AlertDialogFragment
    dialogFragment?.cleanUp()
    dialogFragment?.dismissAllowingStateLoss()
    Timber.d("dismissAlertDialog")
  } catch (e: Exception) {
    Timber.d("dismissAlertDialog $e")
  }
}

class AlertDialogFragment : DialogFragment() {
  var builder: Builder? = null
    private set

  override fun onCancel(dialog: DialogInterface) {
    super.onCancel(dialog)
    builder?.onCancelListener?.onCancel(dialog)
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog
      .Builder(requireContext())
      .apply {
        val builder = this@AlertDialogFragment.builder ?: return@apply

        setTitle(builder.titleText)
        setMessage(builder.messageText)
        setCancelable(builder.cancelable)

        setIcon(builder.iconId)
        setIcon(builder.icon)

        if (builder.negativeButtonText !== null && builder.negativeButtonClickListener !== null) {
          setNegativeButton(
            builder.negativeButtonText,
            builder.negativeButtonClickListener
          )
        }
        if (builder.positiveButtonText !== null && builder.positiveButtonClickListener !== null) {
          setPositiveButton(
            builder.positiveButtonText,
            builder.positiveButtonClickListener
          )
        }
        if (builder.neutralButtonText !== null && builder.neutralButtonClickListener !== null) {
          setNeutralButton(
            builder.neutralButtonText,
            builder.neutralButtonClickListener
          )
        }
      }
      .create()
  }

  fun cleanUp() {
    builder = null
  }

  companion object {
    fun getInstance(builder: Builder): AlertDialogFragment {
      return AlertDialogFragment().apply { this.builder = builder }
    }
  }

  @Suppress("unused")
  class Builder {
    var titleText: String? = null
      private set
    var messageText: String? = null
      private set
    var cancelable: Boolean = true
      private set

    @DrawableRes
    var iconId: Int = 0
      private set
    var icon: Drawable? = null
      private set

    var onCancelListener: DialogInterface.OnCancelListener? = null
      private set

    var negativeButtonText: String? = null
      private set
    var negativeButtonClickListener: DialogInterface.OnClickListener? = null
      private set

    var positiveButtonText: String? = null
      private set
    var positiveButtonClickListener: DialogInterface.OnClickListener? = null
      private set

    var neutralButtonText: String? = null
      private set
    var neutralButtonClickListener: DialogInterface.OnClickListener? = null
      private set

    fun title(title: String) = apply { this.titleText = title }

    fun message(message: String) = apply { this.messageText = message }

    fun cancelable(cancelable: Boolean) = apply { this.cancelable = cancelable }

    fun iconId(@DrawableRes iconId: Int) = apply { this.iconId = iconId }

    fun icon(icon: Drawable) = apply { this.icon = icon }

    fun onCancel(listener: (DialogInterface) -> Unit) {
      this.onCancelListener = DialogInterface.OnCancelListener(listener)
    }

    fun negativeAction(
      text: String,
      listener: (dialog: DialogInterface, which: Int) -> Unit,
    ) = apply {
      this.negativeButtonText = text
      this.negativeButtonClickListener = DialogInterface.OnClickListener(listener)
    }

    fun positiveAction(
      text: String,
      listener: (dialog: DialogInterface, which: Int) -> Unit,
    ) = apply {
      this.positiveButtonText = text
      this.positiveButtonClickListener = DialogInterface.OnClickListener(listener)
    }

    fun neutralAction(
      text: String,
      listener: (dialog: DialogInterface, which: Int) -> Unit,
    ) = apply {
      this.neutralButtonText = text
      this.neutralButtonClickListener = DialogInterface.OnClickListener(listener)
    }

    fun build() = getInstance(this)
  }
}
