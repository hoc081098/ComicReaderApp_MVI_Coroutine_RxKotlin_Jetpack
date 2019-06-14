package com.hoc.comicapp.domain

import com.hoc.comicapp.utils.Either
import com.hoc.comicapp.domain.models.*

interface ComicRepository {
  /**
   * Get top month comics
   */
  suspend fun getTopMonthComics(): Either<ComicAppError, List<TopMonthComic>>

  /**
   * Get recent updated comics
   */
  suspend fun getUpdatedComics(page: Int? = null): Either<ComicAppError, List<UpdatedComic>>

  /**
   * Get suggest comics
   */
  suspend fun getSuggestComics(): Either<ComicAppError, List<SuggestComic>>

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
   * Search comic by [query] (only partial information)
   * @param query term to search
   */
  suspend fun searchComic(query: String): Either<ComicAppError, List<SearchComic>>
}