package com.hoc.comicapp.ui.search_comic

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.home.ComicArg
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.item_recycler_search_comic.view.*

object SearchComicDiffUtilItemCallback : DiffUtil.ItemCallback<Item>() {
  override fun areItemsTheSame(
    oldItem: Item,
    newItem: Item
  ) = when {
    oldItem is Item.ComicItem && newItem is Item.ComicItem -> oldItem.link == newItem.link
    else -> oldItem == newItem
  }

  override fun areContentsTheSame(
    oldItem: Item,
    newItem: Item
  ) = oldItem == newItem
}

class SearchComicAdapter(
  private val glide: GlideRequests,
  private val compositeDisposable: CompositeDisposable
) : ListAdapter<Item, SearchComicAdapter.VH>(SearchComicDiffUtilItemCallback) {
  private val clickComicS = PublishRelay.create<ComicArg>()
  val clickComicObservable get() = clickComicS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, @LayoutRes viewType: Int): VH {
    val view = parent inflate viewType
    return when (viewType) {
      R.layout.item_recycler_search_comic -> ComicVH(view, parent)
      else -> error("Don't know viewType=$viewType")
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  @LayoutRes
  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is Item.ComicItem -> R.layout.item_recycler_search_comic
    }
  }

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: Item)
  }

  inner class ComicVH(itemView: View, parent: View) : VH(itemView) {
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
        .ofType<Item.ComicItem>()
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

    override fun bind(item: Item) {
      if (item !is Item.ComicItem) {
        return
      }

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