package com.hoc.comicapp.domain.repository

import com.hoc.comicapp.domain.DomainResult
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.CategoryDetailPopularComic
import com.hoc.comicapp.domain.models.ChapterDetail
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicDetail

interface ComicRepository {
  /**
   * Get most viewed comics
   * @param page number
   */
  suspend fun getMostViewedComics(page: Int = 1): DomainResult<List<Comic>>

  /**
   * Get recent updated comics
   * @param page number
   */
  suspend fun getUpdatedComics(page: Int = 1): DomainResult<List<Comic>>

  /**
   * Get newest comics
   * @param page number
   */
  suspend fun getNewestComics(page: Int = 1): DomainResult<List<Comic>>

  /**
   * Get triple of newest comics, most viewed comics and updated comics.
   */
  suspend fun refreshAll(): DomainResult<Triple<List<Comic>, List<Comic>, List<Comic>>>

  /**
   * Get comic detail and all chapters
   * @param comicLink comic url
   */
  suspend fun getComicDetail(comicLink: String): DomainResult<ComicDetail>

  /**
   * Get chapter detail (images or html content)
   * @param chapterLink chapter url
   */
  suspend fun getChapterDetail(chapterLink: String): DomainResult<ChapterDetail>

  /**
   * Get all categories
   */
  suspend fun getAllCategories(): DomainResult<List<Category>>

  /**
   * Get category detail by [categoryLink]
   * @param categoryLink category url
   * @param page page number
   * @return List of comics
   */
  suspend fun getCategoryDetail(
    categoryLink: String,
    page: Int = 1,
  ): DomainResult<List<Comic>>

  /**
   * Get category detail by [categoryLink]
   * @param categoryLink category url
   * @return List of comics
   */
  suspend fun getCategoryDetailPopular(categoryLink: String): DomainResult<List<CategoryDetailPopularComic>>

  /**
   * Search comic by [query] (only partial information)
   * @param query term to search
   * @param page page number
   */
  suspend fun searchComic(
    query: String,
    page: Int = 1,
  ): DomainResult<List<Comic>>
}
