package com.hoc.comicapp.domain.repository

import com.hoc.comicapp.domain.models.*
import com.hoc.comicapp.utils.Either

interface ComicRepository {
  /**
   * Get most viewed comics
   * @param page number
   */
  suspend fun getMostViewedComics(page: Int? = null): Either<ComicAppError, List<Comic>>

  /**
   * Get recent updated comics
   * @param page number
   */
  suspend fun getUpdatedComics(page: Int? = null): Either<ComicAppError, List<Comic>>

  /**
   * Get newest comics
   * @param page number
   */
  suspend fun getNewestComics(page: Int? = null): Either<ComicAppError, List<Comic>>

  /**
   * Get comic detail and all chapters
   * @param comicLink comic url
   */
  suspend fun getComicDetail(comicLink: String): Either<ComicAppError, ComicDetail>

  /**
   * Get chapter detail (images or html content)
   * @param chapterLink chapter url
   */
  suspend fun getChapterDetail(chapterLink: String): Either<ComicAppError, ChapterDetail>

  /**
   * Get all categories
   */
  suspend fun getAllCategories(): Either<ComicAppError, List<Category>>

  /**
   * Get category detail by [categoryLink]
   * @param categoryLink category url
   * @param page page number
   * @return List of comics
   */
  suspend fun getCategoryDetail(
    categoryLink: String,
    page: Int? = null
  ): Either<ComicAppError, List<Comic>>

  /**
   * Get category detail by [categoryLink]
   * @param categoryLink category url
   * @return List of comics
   */
  suspend fun getCategoryDetailPopular(categoryLink: String): Either<ComicAppError, List<CategoryDetailPopularComic>>

  /**
   * Search comic by [query] (only partial information)
   * @param query term to search
   * @param page page number
   */
  suspend fun searchComic(
    query: String,
    page: Int? = null
  ): Either<ComicAppError, List<Comic>>
}