package com.hoc.comicapp.ui.search_comic

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.detail.ComicArg
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.hoc.comicapp.utils.mapNotNull
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.ofType
import kotlinx.android.synthetic.main.item_recycler_search_comic.view.*
import kotlinx.android.synthetic.main.item_recycler_search_comic_load_more.view.*

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

  private val clickButtonRetryOrLoadMoreS = PublishRelay.create<Boolean>()
  val clickButtonRetryOrLoadMoreObservable get() = clickButtonRetryOrLoadMoreS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, @LayoutRes viewType: Int): VH {
    val itemView = parent inflate viewType
    return when (viewType) {
      R.layout.item_recycler_search_comic -> ComicVH(itemView, parent)
      R.layout.item_recycler_search_comic_load_more -> LoadMoreVH(itemView, parent)
      else -> error("Don't know viewType=$viewType")
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  @LayoutRes
  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is Item.ComicItem -> R.layout.item_recycler_search_comic
      else -> R.layout.item_recycler_search_comic_load_more
    }
  }

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: Item)
  }

  inner class LoadMoreVH(itemView: View, parent: View) : VH(itemView) {
    private val progressBar = itemView.progress_bar!!
    private val textErrorMessage = itemView.text_error_message!!
    private val buttonRetryOrLoadMore = itemView.button_retry_or_loadmore!!

    init {
      buttonRetryOrLoadMore
        .clicks()
        .takeUntil(parent.detaches())
        .map { adapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .mapNotNull {
          when (getItem(it)) {
            is Item.ComicItem -> null
            Item.Idle -> false
            Item.Loading -> null
            is Item.Error -> true
          }
        }
        .subscribe(clickButtonRetryOrLoadMoreS)
        .addTo(compositeDisposable)
    }

    override fun bind(item: Item) {
      when (item) {
        is Item.ComicItem -> Unit
        Item.Idle -> {
          progressBar.isVisible = false
          textErrorMessage.isVisible = false
          buttonRetryOrLoadMore.isVisible = true
          buttonRetryOrLoadMore.text = "Load next page"
        }
        Item.Loading -> {
          progressBar.isVisible = true
          textErrorMessage.isVisible = false
          buttonRetryOrLoadMore.isVisible = false
        }
        is Item.Error -> {
          progressBar.isVisible = false
          textErrorMessage.isVisible = true
          textErrorMessage.text = item.errorMessage
          buttonRetryOrLoadMore.isVisible = true
          buttonRetryOrLoadMore.text = "Retry"
        }
      }
    }
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
            link = it.link,
            view = it.view,
            remoteThumbnail = it.thumbnail
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
        .placeholder(R.drawable.splash_background)
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