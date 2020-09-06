package com.hoc.comicapp.initializer

import android.content.Context
import androidx.startup.Initializer
import com.hoc.comicapp.data.analytics.FirebaseAnalyticsProvider
import com.hoc.comicapp.data.analytics.SnakeCaseFirebaseAnalyticsEventMapper
import com.hoc.comicapp.domain.analytics.AnalyticsService
import timber.log.Timber

class AnalyticsInitializer : Initializer<Unit> {
  override fun create(context: Context) {
    context
      .startKoinIfNeeded()
      .get<AnalyticsService>()
      .addProvider(
        FirebaseAnalyticsProvider(
          context = context,
          mapper = SnakeCaseFirebaseAnalyticsEventMapper()
        )
      )
    Timber.tag("Initializer").d("Analytics initialized")
  }

  override fun dependencies() = listOf(
    KoinInitializer::class.java,
    TimberInitializer::class.java,
  )
}
