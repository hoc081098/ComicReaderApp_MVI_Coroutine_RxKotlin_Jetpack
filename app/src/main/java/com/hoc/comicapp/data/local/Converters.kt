package com.hoc.comicapp.data.local

import androidx.room.TypeConverter
import com.hoc.comicapp.data.JsonAdaptersContainer
import com.hoc.comicapp.data.local.entities.ComicEntity
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Date

/**
 * Type converters to allow Room to reference complex data types.
 */
class Converters : KoinComponent {
  private val jsonAdapterContainer by inject<JsonAdaptersContainer>()

  @TypeConverter
  fun listStringsToString(strings: List<String>): String {
    return jsonAdapterContainer.listStringsAdapter.toJson(strings)
  }

  @TypeConverter
  fun stringToListStrings(s: String): List<String> {
    return jsonAdapterContainer.listStringsAdapter.fromJson(s) ?: emptyList()
  }

  @TypeConverter
  fun listAuthorsToString(strings: List<ComicEntity.Author>): String {
    return jsonAdapterContainer.listComicEntityAuthorsAdapter.toJson(strings)
  }

  @TypeConverter
  fun stringToListAuthors(s: String): List<ComicEntity.Author> {
    return jsonAdapterContainer.listComicEntityAuthorsAdapter.fromJson(s) ?: emptyList()
  }

  @TypeConverter
  fun listCategoriesToString(strings: List<ComicEntity.Category>): String {
    return jsonAdapterContainer.listComicEntityCategoriesAdapter.toJson(strings)
  }

  @TypeConverter
  fun stringToListCategories(s: String): List<ComicEntity.Category> {
    return jsonAdapterContainer.listComicEntityCategoriesAdapter.fromJson(s) ?: emptyList()
  }

  @TypeConverter
  fun dateToLong(date: Date): Long {
    return date.time
  }

  @TypeConverter
  fun longToDate(time: Long): Date {
    return Date(time)
  }
}
