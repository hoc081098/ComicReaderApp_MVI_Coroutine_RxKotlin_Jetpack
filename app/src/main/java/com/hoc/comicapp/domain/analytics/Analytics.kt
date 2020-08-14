package com.hoc.comicapp.domain.analytics

import androidx.annotation.AnyThread

data class AnalyticsEvent(
  val name: String,
  val params: Map<String, Any>? = null,
) {
  companion object
}

interface AnalyticsService {
  @AnyThread
  fun track(event: AnalyticsEvent)

  @AnyThread
  fun addProvider(provider: AnalyticsProvider)

  @AnyThread
  fun removeProvider(provider: AnalyticsProvider)
}

interface AnalyticsProvider {
  fun track(event: String, params: Map<String, Any>?)
}
