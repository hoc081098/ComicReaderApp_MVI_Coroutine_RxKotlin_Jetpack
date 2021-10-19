package com.hoc.comicapp.data.analytics

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.hoc.comicapp.domain.analytics.AnalyticsEvent
import com.hoc.comicapp.domain.analytics.AnalyticsProvider
import com.hoc.comicapp.domain.analytics.AnalyticsService
import java.util.concurrent.CopyOnWriteArrayList

class AnalyticsServiceImpl : AnalyticsService {
  private val providers = CopyOnWriteArrayList<AnalyticsProvider>()

  override fun addProvider(provider: AnalyticsProvider) {
    providers += provider
  }

  override fun removeProvider(provider: AnalyticsProvider) {
    providers -= provider
  }

  override fun track(event: AnalyticsEvent) {
    providers.forEach { it.track(event.name, event.params) }
  }
}

class SnakeCaseFirebaseAnalyticsEventMapper {
  fun nameFor(event: String): String =
    CAMEL_REGEX.replace(event) { "_" + it.value }.lowercase()

  fun paramsFor(params: Map<String, Any>?): Bundle? {
    return bundleOf(
      *(params ?: return null)
        .mapKeys { nameFor(it.key) }
        .toList()
        .toTypedArray()
    )
  }

  private companion object {
    val CAMEL_REGEX = "(?<=[a-zA-Z])[A-Z]".toRegex()
  }
}

class FirebaseAnalyticsProvider(
  private val context: Context,
  private val mapper: SnakeCaseFirebaseAnalyticsEventMapper,
) : AnalyticsProvider {
  private val firebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context.applicationContext) }

  override fun track(event: String, params: Map<String, Any>?) {
    firebaseAnalytics.logEvent(
      mapper.nameFor(event),
      mapper.paramsFor(params)
    )
  }
}

/*
 * AnalyticsEvents
 */

fun AnalyticsEvent.Companion.downloadChapter(
  chapterLink: String,
  chapterName: String,
  comicLink: String,
  comicName: String,
  elapsedInMilliseconds: Double,
  elapsedInString: String,
): AnalyticsEvent = AnalyticsEvent(
  name = "downloadChapter",
  params = mapOf(
    "chapterLink" to chapterLink,
    "chapterName" to chapterName,
    "comicLink" to comicLink,
    "comicName" to comicName,
    "elapsedInMilliseconds" to elapsedInMilliseconds,
    "elapsedInString" to elapsedInString,
  )
)

fun AnalyticsEvent.Companion.readChapter(
  chapterLink: String,
  chapterName: String,
  imagesSize: Int,
): AnalyticsEvent = AnalyticsEvent(
  name = "readChapter",
  params = mapOf(
    "chapterLink" to chapterLink,
    "chapterName" to chapterName,
    "imagesSize" to imagesSize,
  )
)
