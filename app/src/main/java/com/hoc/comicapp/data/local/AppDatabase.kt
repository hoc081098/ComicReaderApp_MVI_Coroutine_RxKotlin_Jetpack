package com.hoc.comicapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hoc.comicapp.data.local.dao.ChapterDao
import com.hoc.comicapp.data.local.dao.ComicDao
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicEntity

@Database(entities = [ComicEntity::class, ChapterEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun comicDao(): ComicDao
  abstract fun chapterDao(): ChapterDao

  companion object {
    private const val DATABASE_NAME = "com.hoc.comicapp.db"

    // For Singleton instantiation
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return instance ?: synchronized(this) {
        instance ?: buildDatabase(context).also { instance = it }
      }
    }

    private fun buildDatabase(context: Context): AppDatabase {
      return Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()
    }
  }
}
