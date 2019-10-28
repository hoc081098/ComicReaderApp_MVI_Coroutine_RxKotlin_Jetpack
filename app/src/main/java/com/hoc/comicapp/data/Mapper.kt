package com.hoc.comicapp.data

import com.hoc.comicapp.data.firebase.entity._FavoriteComic
import com.hoc.comicapp.data.local.entities.ChapterEntity
import com.hoc.comicapp.data.local.entities.ComicAndChapters
import com.hoc.comicapp.data.local.entities.ComicEntity
import com.hoc.comicapp.data.remote.response.CategoryDetailPopularComicResponse
import com.hoc.comicapp.data.remote.response.CategoryResponse
import com.hoc.comicapp.data.remote.response.ChapterDetailResponse
import com.hoc.comicapp.data.remote.response.ComicDetailResponse
import com.hoc.comicapp.data.remote.response.ComicResponse
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.domain.models.CategoryDetailPopularComic
import com.hoc.comicapp.domain.models.ChapterDetail
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.DownloadedChapter
import com.hoc.comicapp.domain.models.DownloadedComic
import com.hoc.comicapp.domain.models.FavoriteComic

object Mapper {

  /**
   *
   */

  fun domainToFirebaseEntity(comic: FavoriteComic): _FavoriteComic {
    return _FavoriteComic(
      url = comic.url,
      title = comic.title,
      thumbnail = comic.thumbnail,
      view = comic.view,
      createdAt = null
    )
  }

  fun domainToLocalEntity(domain: DownloadedChapter): ChapterEntity {
    return ChapterEntity(
      chapterLink = domain.chapterLink,
      comicLink = domain.comicLink,
      downloadedAt = domain.downloadedAt,
      images = domain.images,
      time = domain.time,
      view = domain.view,
      chapterName = domain.chapterName,
      order = -1
    )
  }

  fun domainToLocalEntity(domain: DownloadedComic): ComicEntity {
    return ComicEntity(
      comicLink = domain.comicLink,
      view = domain.view,
      categories = domain.categories.map {
        ComicEntity.Category(
          link = it.link,
          name = it.name
        )
      },
      authors = domain.authors.map {
        ComicEntity.Author(
          link = it.link,
          name = it.name
        )
      },
      thumbnail = domain.thumbnail,
      lastUpdated = domain.lastUpdated,
      shortenedContent = domain.shortenedContent,
      title = domain.title
    )
  }

  /**
   *
   */

  fun entityToDomainModel(entity: ChapterEntity): DownloadedChapter {
    return DownloadedChapter(
      chapterName = entity.chapterName,
      view = entity.view,
      time = entity.time,
      chapterLink = entity.chapterLink,
      images = entity.images,
      downloadedAt = entity.downloadedAt,
      comicLink = entity.comicLink,
      prevChapterLink = null,
      nextChapterLink = null,
      chapters = emptyList()
    )
  }

  fun entityToDomainModel(entity: ComicAndChapters): DownloadedComic {
    val comic = entity.comic
    val chapters = entity.chapters

    return DownloadedComic(
      title = comic.title,
      view = comic.view,
      comicLink = comic.comicLink,
      lastUpdated = comic.lastUpdated,
      shortenedContent = comic.shortenedContent,
      thumbnail = comic.thumbnail,
      authors = comic.authors.map {
        DownloadedComic.Author(
          name = it.name,
          link = it.link
        )
      },
      categories = comic.categories.map {
        DownloadedComic.Category(
          name = it.name,
          link = it.link
        )
      },
      chapters = chapters.map {
        DownloadedChapter(
          chapterName = it.chapterName,
          comicLink = it.comicLink,
          view = it.view,
          downloadedAt = it.downloadedAt,
          chapterLink = it.chapterLink,
          images = it.images,
          time = it.time,
          prevChapterLink = null,
          nextChapterLink = null,
          chapters = emptyList()
        )
      }
    )
  }

  /**
   *
   */

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

  fun responseToDomainModel(response: CategoryDetailPopularComicResponse): CategoryDetailPopularComic {
    return CategoryDetailPopularComic(
      title = response.title,
      thumbnail = response.thumbnail,
      link = response.link,
      lastChapter = CategoryDetailPopularComic.LastChapter(
        chapterName = response.lastChapter.chapterName,
        chapterLink = response.lastChapter.chapterLink
      )
    )
  }

  fun responseToLocalEntity(response: ComicDetailResponse): ComicEntity {
    return ComicEntity(
      authors = response.authors.map {
        ComicEntity.Author(
          link = it.link,
          name = it.name
        )
      },
      categories = response.categories.map {
        ComicEntity.Category(
          link = it.link,
          name = it.name
        )
      },
      lastUpdated = response.lastUpdated,
      comicLink = response.link,
      shortenedContent = response.shortenedContent,
      thumbnail = response.thumbnail,
      title = response.title,
      view = response.view
    )
  }

  fun responseToFirebaseEntity(response: ComicResponse): _FavoriteComic {
    return _FavoriteComic(
      url = response.link,
      thumbnail = response.thumbnail,
      createdAt = null,
      view = response.view,
      title = response.title
    )
  }

  fun responseToFirebaseEntity(response: ComicDetailResponse): _FavoriteComic {
    return _FavoriteComic(
      url = response.link,
      thumbnail = response.thumbnail,
      createdAt = null,
      view = response.view,
      title = response.title
    )
  }
}