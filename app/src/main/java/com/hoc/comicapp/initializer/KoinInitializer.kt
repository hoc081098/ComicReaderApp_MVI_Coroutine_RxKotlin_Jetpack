package com.hoc.comicapp.initializer

import android.content.Context
import androidx.startup.Initializer
import com.hoc.comicapp.koin.appModule
import com.hoc.comicapp.koin.dataModule
import com.hoc.comicapp.koin.networkModule
import com.hoc.comicapp.koin.viewModelModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.KoinContextHandler
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

@Suppress("unused")
class KoinInitializer : Initializer<Koin> {
  @ExperimentalCoroutinesApi
  @ObsoleteCoroutinesApi
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
@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
fun Context.startKoinIfNeeded(): Koin {
  return KoinContextHandler.getOrNull() ?: startKoin {
    // use AndroidLogger as Koin Logger
    // androidLogger(level = if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
    // TODO: https://github.com/InsertKoinIO/koin/issues/847
    androidLogger(level = Level.ERROR)

    // use the Android context given there
    androidContext(applicationContext)

    modules(
      listOf(
        networkModule,
        dataModule,
        appModule,
        viewModelModule
      )
    )
  }.koin
}