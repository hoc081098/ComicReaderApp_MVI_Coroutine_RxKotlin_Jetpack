package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.utils.fold
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.rx2.rxObservable

@ExperimentalCoroutinesApi
class ComicDetailInteractorImpl(private val comicRepository: ComicRepository) :
  ComicDetailInteractor {
  override fun getComicDetail(
    coroutineScope: CoroutineScope,
    link: String,
    name: String,
    thumbnail: String
  ): Observable<ComicDetailPartialChange> {
    return coroutineScope.rxObservable<ComicDetailPartialChange> {
      send(
        ComicDetailPartialChange.InitialPartialChange.InitialData(
          initialComic = ComicDetail.InitialComic(
            title = name,
            thumbnail = thumbnail,
            link = link
          )
        )
      )
      send(ComicDetailPartialChange.InitialPartialChange.Loading)

      comicRepository
        .getComicDetail(link)
        .fold({ ComicDetailPartialChange.InitialPartialChange.Error(it) },
          { comic ->
            ComicDetail.Comic(
              title = comic.title,
              link = comic.link,
              view = comic.view!!,
              status = comic.moreDetail!!.status,
              shortenedContent = comic.moreDetail.shortenedContent,
              otherName = comic.moreDetail.otherName,
              categories = comic.moreDetail.categories.map {
                Category(
                  link = it.link,
                  name = it.name
                )
              },
              author = comic.moreDetail.author,
              lastUpdated = comic.moreDetail.lastUpdated,
              thumbnail = comic.thumbnail,
              chapters = comic.chapters.map {
                Chapter(
                  name = it.chapterName,
                  link = it.chapterLink,
                  time = it.time,
                  view = it.view
                )
              }
            ).let { ComicDetailPartialChange.InitialPartialChange.Data(it) }
          })
        .let { send(it) }
    }
  }
}