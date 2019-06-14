package com.hoc.comicapp.data.remote

import com.hoc.comicapp.data.remote.response.*
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query

const val COMIC_BASE_URL = "https://comic-app-081098.herokuapp.com"

interface ComicApiService {
  /**
   *
   */

  @GET("top_month_comics")
  suspend fun getTopMonthComics(): List<TopMonthComicResponse>

  @GET("updated_comics")
  suspend fun getUpdatedComics(@Query("page") page: Int? = null): List<UpdatedComicResponse>

  @GET("suggest_comics")
  suspend fun getSuggestComics(): List<SuggestComicResponse>

  /**
   *
   */

  @GET("comic_detail")
  suspend fun getComicDetail(@Query("link") comicLink: String): ComicDetailResponse

  @GET("chapter_detail")
  suspend fun getChapterDetail(@Query("link") chapterLink: String): ChapterDetailResponse

  /**
   *
   */

  @GET("category")
  suspend fun getAllCategories(): List<CategoryResponse>

  @GET("search_comic")
  suspend fun searchComic(@Query("query") query: String): List<SearchComicResponse>

  /**
   *
   */
  companion object {
    operator fun invoke(retrofit: Retrofit) = retrofit.create<ComicApiService>()
  }
}