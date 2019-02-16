package com.hoc.comicapp.data

import com.hoc.comicapp.data.models.Chapter
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.data.remote.response.ChapterResponse
import com.hoc.comicapp.data.remote.response.ComicResponse

object Mapper {
  @JvmStatic
  fun comicResponseToComicModel(comicResponse: ComicResponse): Comic {
    return Comic(
      link = comicResponse.link,
      chapters = comicResponse.chapters.map(::chapterResponseToChapterModel),
      thumbnail = comicResponse.thumbnail,
      title = comicResponse.title,
      view = comicResponse.view
    )
  }

  @JvmStatic
  fun chapterResponseToChapterModel(chapterResponse: ChapterResponse): Chapter {
    return Chapter(
      chapterLink = chapterResponse.chapterLink,
      chapterName = chapterResponse.chapterName,
      time = chapterResponse.time
    )
  }

}