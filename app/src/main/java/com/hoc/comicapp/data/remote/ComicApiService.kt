package com.hoc.comicapp.data.remote

import com.hoc.comicapp.data.remote.response.ComicResponse
import kotlinx.coroutines.Deferred
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

const val COMIC_BASE_URL = "https://comic-app-081098.herokuapp.com"

interface ComicApiService {

  @GET("top_thang")
  fun topMonth(): Deferred<List<ComicResponse>>

  @GET("truyen_moi_cap_nhat")
  fun update(@Query("page") page: Int? = null): Deferred<List<ComicResponse>>

  @GET("truyen_de_cu")
  fun suggest(): Deferred<List<ComicResponse>>

  companion object {
    operator fun invoke(retrofit: Retrofit): ComicApiService =
      retrofit.create(ComicApiService::class.java)
  }
}