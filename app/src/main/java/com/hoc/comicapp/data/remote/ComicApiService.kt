package com.hoc.comicapp.data.remote

import com.hoc.comicapp.data.remote.response.ComicResponse
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

const val COMIC_BASE_URL = "https://comic-app-081098.herokuapp.com"

interface ComicApiService {

  @GET("top_thang") suspend fun topMonth(): List<ComicResponse>

  @GET("truyen_moi_cap_nhat")
  suspend fun update(@Query("page") page: Int? = null): List<ComicResponse>

  @GET("truyen_de_cu") suspend fun suggest(): List<ComicResponse>

  @GET("comic_detail") suspend fun comicDetail(@Query("link") link: String): ComicResponse

  companion object {
    operator fun invoke(retrofit: Retrofit) = retrofit.create<ComicApiService>()
  }
}