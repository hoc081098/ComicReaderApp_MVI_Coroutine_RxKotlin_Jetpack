package com.hoc.comicapp.data

import com.hoc.comicapp.data.remote.response.*
import com.hoc.comicapp.domain.models.*

object Mapper {
  fun responseToDomainModel(response: TopMonthComicResponse): TopMonthComic {
    return TopMonthComic(
      title = response.title,
      link = response.link,
      view = response.view,
      lastChapter = TopMonthComic.Chapter(
        chapterLink = response.lastChapter.chapterLink,
        chapterName = response.lastChapter.chapterName
      ),
      thumbnail = response.thumbnail
    )
  }

  fun responseToDomainModel(response: UpdatedComicResponse): UpdatedComic {
    return UpdatedComic(
      title = response.title,
      link = response.link,
      view = response.view,
      thumbnail = response.thumbnail,
      lastChapters = response.lastChapters.map {
        UpdatedComic.Chapter(
          chapterName = it.chapterName,
          chapterLink = it.chapterLink,
          time = it.time
        )
      }
    )
  }

  fun responseToDomainModel(response: SuggestComicResponse): SuggestComic {
    val lastChapter = response.lastChapter
    return SuggestComic(
      title = response.title,
      link = response.link,
      thumbnail = response.thumbnail,
      lastChapter = SuggestComic.Chapter(
        chapterName = lastChapter.chapterName,
        chapterLink = lastChapter.chapterLink,
        time = lastChapter.time
      )
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
      author = response.author,
      categories = response.categories.map {
        ComicDetail.Category(
          link = it.link,
          name = it.name
        )
      },
      lastUpdated = response.lastUpdated,
      otherName = response.otherName,
      shortenedContent = response.shortenedContent,
      status = response.status
    )
  }

  fun responseToDomainModel(response: ChapterDetailResponse): ChapterDetail {
    return ChapterDetail(
      chapterName = response.chapterName,
      time = response.time,
      chapterLink = response.chapterLink,
      htmlContent = response.htmlContent,
      images = response.images
    )
  }

  fun responseToDomainModel(response: CategoryResponse): Category {
    return Category(
      link = response.link,
      name = response.name,
      description = response.description
    )
  }

  fun responseToDomainModel(response: SearchComicResponse): SearchComic {
    return SearchComic(
      link = response.link,
      title = response.title,
      thumbnail = response.thumbnail,
      categoryNames = response.categoryNames,
      lastChapterName = response.lastChapterName
    )
  }
}