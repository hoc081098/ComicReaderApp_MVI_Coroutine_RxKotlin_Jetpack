package com.hoc.comicapp.data

import com.hoc.comicapp.data.models.Comic

interface ComicRepository {
  suspend fun getTopMonth(): List<Comic>

  suspend fun getUpdate(page: Int? = null): List<Comic>

  suspend fun getSuggest(): List<Comic>
}