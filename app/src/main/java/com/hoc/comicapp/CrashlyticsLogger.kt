@file:Suppress("SpellCheckingInspection")

package com.hoc.comicapp

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.google.firebase.ktx.Firebase
import timber.log.Timber

class CrashlyticsLogger : Timber.Tree() {
  private val crashlytics by lazy { Firebase.crashlytics }

  override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    t ?: return
    crashlytics.run {
      setCustomKeys {
        key(
          "priority",
          when (priority) {
            Log.INFO -> "Log.INFO"
            Log.WARN -> "Log.WARN"
            Log.ERROR -> "Log.ERROR"
            Log.ASSERT -> "Log.ASSERT"
            else -> "Log.$priority"
          }
        )
        key("tag", tag?.ifEmpty { "<empty>" } ?: "<null>")
        key("message", message.ifEmpty { "<empty>" })
        key("throwable_message", t.message ?: "<null>")
      }
      log("[$tag]: $message")
      recordException(t)
    }
  }
}
