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
import com.hoc.comicapp.databinding.ItemRecyclerSearchComicBinding
import com.hoc.comicapp.databinding.ItemRecyclerSearchComicLoadMoreBinding
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewState.Item
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflater
import com.hoc.comicapp.utils.mapNotNull
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.detaches
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.ofType

object SearchComicDiffUtilItemCallback : DiffUtil.ItemCallback<Item>() {
  override fun areItemsTheSame(
    oldItem: Item,
    newItem: Item,
  ) = when {
    oldItem is Item.ComicItem && newItem is Item.ComicItem -> oldItem.link == newItem.link
    else -> oldItem == newItem
  }

  override fun areContentsTheSame(
    oldItem: Item,
    newItem: Item,
  ) = oldItem == newItem
}

class SearchComicAdapter(
  private val glide: GlideRequests,
  private val compositeDisposable: CompositeDisposable,
) : ListAdapter<Item, SearchComicAdapter.VH>(SearchComicDiffUtilItemCallback) {
  private val clickComicS = PublishRelay.create<Arguments.ComicDetailArgs>()
  val clickComicObservable get() = clickComicS.asObservable()

  private val clickButtonRetryOrLoadMoreS = PublishRelay.create<Boolean>()
  val clickButtonRetryOrLoadMoreObservable get() = clickButtonRetryOrLoadMoreS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, @LayoutRes viewType: Int): VH {
    return when (viewType) {
      R.layout.item_recycler_search_comic -> ComicVH(
        ItemRecyclerSearchComicBinding.inflate(
          parent.inflater,
          parent,
          false
        ),
        parent
      )
      R.layout.item_recycler_search_comic_load_more -> LoadMoreVH(
        ItemRecyclerSearchComicLoadMoreBinding.inflate(
          parent.inflater,
          parent,
          false
        ),
        parent
      )
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

  inner class LoadMoreVH(
    private val binding: ItemRecyclerSearchComicLoadMoreBinding,
    parent: View,
  ) : VH(binding.root) {
    init {
      binding.buttonRetryOrLoadmore
        .clicks()
        .takeUntil(parent.detaches())
        .map { bindingAdapterPosition }
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

    override fun bind(item: Item) = binding.run {
      when (item) {
        is Item.ComicItem -> Unit
        Item.Idle -> {
          progressBar.isVisible = false
          textErrorMessage.isVisible = false
          buttonRetryOrLoadmore.isVisible = true
          buttonRetryOrLoadmore.text = "Load next page"
        }
        Item.Loading -> {
          progressBar.isVisible = true
          textErrorMessage.isVisible = false
          buttonRetryOrLoadmore.isVisible = false
        }
        is Item.Error -> {
          progressBar.isVisible = false
          textErrorMessage.isVisible = true
          textErrorMessage.text = item.errorMessage
          buttonRetryOrLoadmore.isVisible = true
          buttonRetryOrLoadmore.text = "Retry"
        }
      }
    }
  }

  inner class ComicVH(private val binding: ItemRecyclerSearchComicBinding, parent: View) :
    VH(binding.root) {
    init {
      itemView
        .clicks()
        .takeUntil(parent.detaches())
        .map { bindingAdapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .map { getItem(it) }
        .ofType<Item.ComicItem>()
        .map {
          Arguments.ComicDetailArgs(
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

    override fun bind(item: Item) = binding.run {
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
      textViewLastChapterName.text = lastChapter?.chapterName
      textCategoryNames.text = lastChapter?.time
    }
  }
}
