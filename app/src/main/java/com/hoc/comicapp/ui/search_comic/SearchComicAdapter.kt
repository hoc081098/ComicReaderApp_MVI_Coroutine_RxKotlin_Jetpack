package com.hoc.comicapp.ui.search_comic

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R.layout.item_recycler_search_comic
import com.hoc.comicapp.ui.home.ComicArg
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.ComicItem
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.item_recycler_search_comic.view.*

object SearchComicDiffUtilItemCallback : DiffUtil.ItemCallback<ComicItem>() {
  override fun areItemsTheSame(
    oldItem: ComicItem,
    newItem: ComicItem
  ) = oldItem.link == newItem.link

  override fun areContentsTheSame(
    oldItem: ComicItem,
    newItem: ComicItem
  ) = oldItem == newItem
}

class SearchComicAdapter(
  private val glide: GlideRequests,
  private val compositeDisposable: CompositeDisposable
) : ListAdapter<ComicItem, SearchComicAdapter.VH>(SearchComicDiffUtilItemCallback) {
  private val clickComicS = PublishRelay.create<ComicArg>()
  val clickComicObservable get() = clickComicS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate item_recycler_search_comic, parent)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View, parent: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textLastChapterName = itemView.text_view_last_chapter_name!!
    private val textComicName = itemView.text_comic_name!!
    private val textCategoryNames = itemView.text_category_names!!

    init {
      itemView
        .clicks()
        .takeUntil(parent.detaches())
        .map { adapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .map { getItem(it) }
        .map {
          ComicArg(
            title = it.title,
            thumbnail = it.thumbnail,
            link = it.link
          )
        }
        .subscribe(clickComicS)
        .addTo(compositeDisposable)
    }

    fun bind(item: ComicItem) {
      glide
        .load(item.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)

      textComicName.text = item.title
      val lastChapter = item.lastChapters.lastOrNull()
      textLastChapterName.text = lastChapter?.chapterName
      textCategoryNames.text = lastChapter?.time
    }
  }
}