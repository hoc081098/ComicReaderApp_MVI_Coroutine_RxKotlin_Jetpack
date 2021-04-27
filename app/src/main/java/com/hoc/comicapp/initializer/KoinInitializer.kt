package com.hoc.comicapp.initializer

import android.content.Context
import androidx.startup.Initializer
import com.hoc.comicapp.koin.appModule
import com.hoc.comicapp.koin.dataModule
import com.hoc.comicapp.koin.navigationModule
import com.hoc.comicapp.koin.networkModule
import com.hoc.comicapp.koin.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

@Suppress("unused")
class KoinInitializer : Initializer<Koin> {
  override fun create(context: Context): Koin = context
    .startKoinIfNeeded()
    .also { Timber.tag("Initializer").d("Koin initialized") }

  override fun dependencies(): List<Class<out Initializer<*>>> =
    listOf(TimberInitializer::class.java)
}

/**
 * Start koin if global KoinContext is null.
 * @return [Koin] instance.
 */
fun Context.startKoinIfNeeded(): Koin {
  return GlobalContext.getOrNull() ?: startKoin {
    // use AndroidLogger as Koin Logger
    // TODO(Koin): androidLogger(level = if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
    androidLogger(level = Level.NONE)

    // use the Android context given there
    androidContext(applicationContext)

    modules(
      listOf(
        networkModule,
        dataModule,
        appModule,
        viewModelModule,
        navigationModule,
      )
    )
  }.koin
}
