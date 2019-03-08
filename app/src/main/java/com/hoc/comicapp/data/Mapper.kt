package com.hoc.comicapp.data

import com.hoc.comicapp.data.models.Category
import com.hoc.comicapp.data.models.Chapter
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.models.MoreDetail
import com.hoc.comicapp.data.remote.response.CategoryResponse
import com.hoc.comicapp.data.remote.response.ChapterResponse
import com.hoc.comicapp.data.remote.response.ComicResponse
import com.hoc.comicapp.data.remote.response.MoreDetailResponse

object Mapper {
  @JvmStatic
  fun comicResponseToComicModel(comicResponse: ComicResponse): Comic {
    return Comic(
      link = comicResponse.link,
      chapters = comicResponse.chapters.map(::chapterResponseToChapterModel),
      thumbnail = comicResponse.thumbnail,
      title = comicResponse.title,
      view = comicResponse.view,
      moreDetail = moreDetailResponseToMoreDetailModel(comicResponse.moreDetail)
    )
  }

  @JvmStatic
  fun moreDetailResponseToMoreDetailModel(moreDetailResponse: MoreDetailResponse?): MoreDetail? {
    return moreDetailResponse?.let {
      MoreDetail(
        lastUpdated = it.lastUpdated,
        author = it.author,
        categories = it.categories.map(Mapper::categoryResponseToCategoryModel),
        otherName = it.otherName,
        shortenedContent = it.shortenedContent,
        status = it.status
      )
    }
  }

  @JvmStatic
  fun categoryResponseToCategoryModel(categoryResponse: CategoryResponse): Category {
    return Category(
      name = categoryResponse.name,
      link = categoryResponse.link
    )
  }

  @JvmStatic
  fun chapterResponseToChapterModel(chapterResponse: ChapterResponse): Chapter {
    return Chapter(
      chapterLink = chapterResponse.chapterLink,
      chapterName = chapterResponse.chapterName,
      time = chapterResponse.time,
      view = chapterResponse.view,
      images = chapterResponse.images
    )
  }
}