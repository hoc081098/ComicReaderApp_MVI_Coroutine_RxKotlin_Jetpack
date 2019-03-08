package com.hoc.comicapp.ui.detail

import com.hoc.comicapp.data.ComicRepository
import com.hoc.comicapp.utils.fold
import io.reactivex.ObservableSource
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
  ): ObservableSource<ComicDetailPartialChange> {
    return coroutineScope.rxObservable<ComicDetailPartialChange> {
      send(ComicDetailPartialChange.InitialPartialChange.InitialData(name, thumbnail))
      send(ComicDetailPartialChange.InitialPartialChange.Loading)

      comicRepository
        .getComicDetail(link)
        .fold({ ComicDetailPartialChange.InitialPartialChange.Error(it) },
          { comic ->
            ComicDetail(
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
              thumbnail = comic.thumbnail
            ).let { ComicDetailPartialChange.InitialPartialChange.Data(it) }
          })
        .let { send(it) }
    }
  }
}