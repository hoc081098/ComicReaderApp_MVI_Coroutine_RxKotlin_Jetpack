package com.hoc.comicapp.data.local

import androidx.room.TypeConverter
import com.hoc.comicapp.data.local.entities.ComicEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Type converters to allow Room to reference complex data types.
 */
class Converters : KoinComponent {
  private val moshi by inject<Moshi>()
  private val adapterListString by lazy {
    moshi.adapter<List<String>>(
      Types.newParameterizedType(
        List::class.java,
        String::class.java
      )
    )
  }
  private val adapterListAuthor by lazy {
    moshi.adapter<List<ComicEntity.Author>>(
      Types.newParameterizedType(
        List::class.java,
        ComicEntity.Author::class.java
      )
    )
  }
  private val adapterListCategory by lazy {
    moshi.adapter<List<ComicEntity.Category>>(
      Types.newParameterizedType(
        List::class.java,
        ComicEntity.Category::class.java
      )
    )
  }

  @TypeConverter
  fun listStringsToString(strings: List<String>): String {
    return adapterListString.toJson(strings)
  }

  @TypeConverter
  fun stringToListStrings(s: String): List<String> {
    return adapterListString.fromJson(s) ?: emptyList()
  }

  @TypeConverter
  fun listAuthorsToString(strings: List<ComicEntity.Author>): String {
    return adapterListAuthor.toJson(strings)
  }

  @TypeConverter
  fun stringToListAuthors(s: String): List<ComicEntity.Author> {
    return adapterListAuthor.fromJson(s) ?: emptyList()
  }

  @TypeConverter
  fun listCategoriesToString(strings: List<ComicEntity.Category>): String {
    return adapterListCategory.toJson(strings)
  }

  @TypeConverter
  fun stringToListCategories(s: String): List<ComicEntity.Category> {
    return adapterListCategory.fromJson(s) ?: emptyList()
  }

}