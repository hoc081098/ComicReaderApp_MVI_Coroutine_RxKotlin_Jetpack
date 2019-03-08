package com.hoc.comicapp.data

import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.ComicAppError
import com.hoc.comicapp.utils.Either

interface ComicRepository {
  /**
   * Get top month comics (only partial information)
   */
  suspend fun getTopMonth(): Either<ComicAppError, List<Comic>>

  /**
   * Get recent update comics (only partial information)
   */
  suspend fun getUpdate(page: Int? = null): Either<ComicAppError, List<Comic>>

  /**
   * Get suggest comics (only partial information)
   */
  suspend fun getSuggest(): Either<ComicAppError, List<Comic>>

  /**
   * Get comic detail and all chapters (only chapter name and chapter link)
   * @param comicLink comic url
   */
  suspend fun getComicDetail(comicLink: String): Either<ComicAppError, Comic>
}