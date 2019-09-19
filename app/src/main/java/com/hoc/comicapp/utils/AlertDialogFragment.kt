package com.hoc.comicapp.utils

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity

fun FragmentActivity.showAlertDialog(init: AlertDialogFragment.Builder.() -> Unit): AlertDialogFragment {
  return AlertDialogFragment.Builder()
    .apply(init)
    .build()
    .apply { show(supportFragmentManager, AlertDialogFragment::class.java.simpleName) }
}

class AlertDialogFragment(private val builder: Builder) : DialogFragment() {
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return AlertDialog
      .Builder(requireContext())
      .apply {
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

  companion object {
    fun getInstance(builder: Builder): AlertDialogFragment {
      return AlertDialogFragment(builder)
    }
  }

  class Builder {
    var titleText: String? = null
    var messageText: String? = null
    var cancelable: Boolean = true

    @DrawableRes
    var iconId: Int = 0
    var icon: Drawable? = null

    var negativeButtonText: String? = null
    var negativeButtonClickListener: DialogInterface.OnClickListener? = null

    var positiveButtonText: String? = null
    var positiveButtonClickListener: DialogInterface.OnClickListener? = null

    var neutralButtonText: String? = null
    var neutralButtonClickListener: DialogInterface.OnClickListener? = null

    fun title(title: String) = apply { this.titleText = title }

    fun message(message: String) = apply { this.messageText = message }

    fun cancelable(cancelable: Boolean) = apply { this.cancelable = cancelable }

    fun iconId(@DrawableRes iconId: Int) = apply { this.iconId = iconId }

    fun icon(icon: Drawable) = apply { this.icon = icon }

    fun negativeAction(
      text: String,
      listener: (dialog: DialogInterface, which: Int) -> Unit
    ) = apply {
      this.negativeButtonText = text
      this.negativeButtonClickListener = DialogInterface.OnClickListener(listener)
    }

    fun positiveAction(
      text: String,
      listener: (dialog: DialogInterface, which: Int) -> Unit
    ) = apply {
      this.positiveButtonText = text
      this.positiveButtonClickListener = DialogInterface.OnClickListener(listener)
    }

    fun neutralAction(
      text: String,
      listener: (dialog: DialogInterface, which: Int) -> Unit
    ) = apply {
      this.neutralButtonText = text
      this.neutralButtonClickListener = DialogInterface.OnClickListener(listener)
    }

    fun build() = getInstance(this)
  }
}