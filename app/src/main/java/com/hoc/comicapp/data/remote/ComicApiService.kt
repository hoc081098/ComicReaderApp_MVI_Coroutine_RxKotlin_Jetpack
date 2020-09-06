package com.hoc.comicapp.data.remote

import com.hoc.comicapp.data.remote.response.CategoryDetailPopularComicResponse
import com.hoc.comicapp.data.remote.response.CategoryResponse
import com.hoc.comicapp.data.remote.response.ChapterDetailResponse
import com.hoc.comicapp.data.remote.response.ComicDetailResponse
import com.hoc.comicapp.data.remote.response.ComicResponse
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

const val COMIC_BASE_URL = "https://comic-app-081098.herokuapp.com"

interface ComicApiService {
  /**
   *
   */

  @GET("most_viewed_comics")
  suspend fun getMostViewedComics(@Query("page") page: Int?): List<ComicResponse>

  @GET("updated_comics")
  suspend fun getUpdatedComics(@Query("page") page: Int?): List<ComicResponse>

  @GET("newest_comics")
  suspend fun getNewestComics(@Query("page") page: Int?): List<ComicResponse>

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

  @GET("category_detail")
  suspend fun getCategoryDetail(
    @Query("link") categoryLink: String,
    @Query("page") page: Int?,
  ): List<ComicResponse>

  @GET("category_detail/popular")
  suspend fun getCategoryDetailPopular(@Query("link") categoryLink: String): List<CategoryDetailPopularComicResponse>

  @GET("search_comic")
  suspend fun searchComic(
    @Query("query") query: String,
    @Query("page") page: Int?,
  ): List<ComicResponse>

  /**
   *
   */

  @GET
  @Streaming
  suspend fun downloadFile(@Url fileUrl: String): ResponseBody

  /**
   *
   */
  companion object {
    operator fun invoke(retrofit: Retrofit) = retrofit.create<ComicApiService>()
  }
}
