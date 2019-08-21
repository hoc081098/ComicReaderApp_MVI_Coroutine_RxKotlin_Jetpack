package com.hoc.comicapp.data

import com.hoc.comicapp.data.remote.response.CategoryResponse
import com.hoc.comicapp.data.remote.response.ChapterDetailResponse
import com.hoc.comicapp.data.remote.response.ComicDetailResponse
import com.hoc.comicapp.data.remote.response.ComicResponse
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.ChapterDetail
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicDetail

object Mapper {
  fun responseToDomainModel(response: ComicResponse): Comic {
    return Comic(
      title = response.title,
      thumbnail = response.thumbnail,
      link = response.link,
      view = response.view,
      lastChapters = response.lastChapters.map {
        Comic.LastChapter(
          chapterLink = it.chapterLink,
          chapterName = it.chapterName,
          time = it.time
        )
      }
    )
  }

  fun responseToDomainModel(response: ComicDetailResponse): ComicDetail {
    return ComicDetail(
      link = response.link,
      thumbnail = response.thumbnail,
      view = response.view,
      title = response.title,
      chapters = response.chapters.map {
        ComicDetail.Chapter(
          chapterLink = it.chapterLink,
          view = it.view,
          time = it.time,
          chapterName = it.chapterName
        )
      },
      authors = response.authors.map {
        ComicDetail.Author(
          link = it.link,
          name = it.name
        )
      },
      categories = response.categories.map {
        ComicDetail.Category(
          link = it.link,
          name = it.name
        )
      },
      lastUpdated = response.lastUpdated,
      shortenedContent = response.shortenedContent,
      relatedComics = response.relatedComics.map(::responseToDomainModel)
    )
  }

  fun responseToDomainModel(response: ChapterDetailResponse): ChapterDetail {
    return ChapterDetail(
      chapterName = response.chapterName,
      chapterLink = response.chapterLink,
      images = response.images,
      chapters = response.chapters.map {
        ChapterDetail.Chapter(
          chapterLink = it.chapterLink,
          chapterName = it.chapterName
        )
      },
      nextChapterLink = response.nextChapterLink,
      prevChapterLink = response.prevChapterLink
    )
  }

  fun responseToDomainModel(response: CategoryResponse): Category {
    return Category(
      link = response.link,
      name = response.name,
      description = response.description,
      thumbnail = response.thumbnail
    )
  }
}