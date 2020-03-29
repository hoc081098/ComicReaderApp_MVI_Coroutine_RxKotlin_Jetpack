package com.hoc.comicapp.koin

import com.hoc.comicapp.BuildConfig
import com.hoc.comicapp.data.remote.COMIC_BASE_URL
import com.hoc.comicapp.data.remote.ComicApiService
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
  single { getOkHttpClient() }

  single { getRetrofit(get(), get()) }

  single { getComicApiService(get()) }
}

private fun getComicApiService(retrofit: Retrofit): ComicApiService {
  return ComicApiService(retrofit)
}

private fun getRetrofit(client: OkHttpClient, moshi: Moshi): Retrofit {
  return Retrofit.Builder()
    .baseUrl(COMIC_BASE_URL)
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
}

private fun getOkHttpClient(): OkHttpClient {
  return OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .apply {
      if (BuildConfig.DEBUG) {
        HttpLoggingInterceptor()
          .apply { this.level = HttpLoggingInterceptor.Level.BODY }
          .let(::addInterceptor)
      }
    }
    .build()
}