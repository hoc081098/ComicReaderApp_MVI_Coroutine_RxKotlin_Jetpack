package com.hoc.comicapp.koin

import com.hoc.comicapp.BuildConfig
import com.hoc.comicapp.ImageHeaders
import com.hoc.comicapp.data.remote.COMIC_BASE_URL
import com.hoc.comicapp.data.remote.ComicApiService
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private val provideRetrofit = { client: OkHttpClient, moshi: Moshi ->
  Retrofit.Builder()
    .baseUrl(COMIC_BASE_URL)
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
}

private val provideOkHttpClient = {
  OkHttpClient.Builder()
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
    .addInterceptor { chain ->
      val request = chain.request()
      val headers = ImageHeaders.headersFor(request.url.toString())
      if (headers.isEmpty()) {
        chain.proceed(request)
      } else {
        request
          .newBuilder()
          .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
          .build()
          .let(chain::proceed)
      }
    }
    .build()
}

val networkModule = module {
  singleOf(provideOkHttpClient)

  singleOf(provideRetrofit)

  singleOf(ComicApiService::invoke)
}
