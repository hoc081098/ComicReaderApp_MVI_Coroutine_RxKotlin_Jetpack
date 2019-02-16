package com.hoc.comicapp.koin

import com.hoc.comicapp.BuildConfig
import com.hoc.comicapp.data.remote.COMIC_BASE_URL
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
  single { getOkHttpClient() }

  single { getRetrofit(get()) }
}

private fun getRetrofit(client: OkHttpClient): Retrofit {
  return Retrofit.Builder()
    .baseUrl(COMIC_BASE_URL)
    .client(client)
    .addConverterFactory(
      MoshiConverterFactory.create(
        Moshi
          .Builder()
          .add(KotlinJsonAdapterFactory())
          .build()
      )
    )
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .build()
}

private fun getOkHttpClient(): OkHttpClient {
  return OkHttpClient.Builder()
    .apply {
      if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor()
          .setLevel(HttpLoggingInterceptor.Level.BODY)
          .let(::addInterceptor)
      }
    }
    .build()
}