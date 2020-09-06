@file:Suppress("SpellCheckingInspection")

package com.hoc.comicapp

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsLogger : Timber.Tree() {
  private val crashlytics by lazy { FirebaseCrashlytics.getInstance() }

  override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.INFO

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    if (t != null && !tag.isNullOrBlank() && message.isNotBlank()) {
      crashlytics.log("[tag=$tag] [message=$message] [throwable=$t]")
      crashlytics.recordException(t)
    }
  }
}
