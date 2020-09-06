package com.hoc.comicapp.data

import com.hoc.comicapp.data.local.entities.ComicEntity
import com.hoc.comicapp.domain.models.ComicDetail
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class JsonAdaptersContainer(moshi: Moshi) {
  val comicDetailChapterAdapter: JsonAdapter<ComicDetail.Chapter> by lazy {
    moshi.adapter(ComicDetail.Chapter::class.java)
  }

  val listStringsAdapter: JsonAdapter<List<String>> by lazy {
    moshi.adapter<List<String>>(
      Types.newParameterizedType(
        List::class.java,
        String::class.java
      )
    )
  }

  val listComicEntityAuthorsAdapter: JsonAdapter<List<ComicEntity.Author>> by lazy {
    moshi.adapter<List<ComicEntity.Author>>(
      Types.newParameterizedType(
        List::class.java,
        ComicEntity.Author::class.java
      )
    )
  }

  val listComicEntityCategoriesAdapter: JsonAdapter<List<ComicEntity.Category>> by lazy {
    moshi.adapter<List<ComicEntity.Category>>(
      Types.newParameterizedType(
        List::class.java,
        ComicEntity.Category::class.java
      )
    )
  }
}
