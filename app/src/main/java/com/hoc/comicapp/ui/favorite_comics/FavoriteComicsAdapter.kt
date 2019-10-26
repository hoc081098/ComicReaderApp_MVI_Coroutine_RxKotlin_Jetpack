package com.hoc.comicapp.ui.favorite_comics

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ComicItem
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.item_recycler_favorite_comics.view.*
import java.text.SimpleDateFormat
import java.util.*

object FavoriteComicItemDiffUtilItemCallback : DiffUtil.ItemCallback<ComicItem>() {
  override fun areItemsTheSame(oldItem: ComicItem, newItem: ComicItem) = oldItem.url == newItem.url
  override fun areContentsTheSame(oldItem: ComicItem, newItem: ComicItem) = oldItem == newItem
}

class FavoriteComicsAdapter(
  private val glide: GlideRequests,
  private val viewBinderHelper: ViewBinderHelper,
  private val compositeDisposable: CompositeDisposable
) :
  ListAdapter<ComicItem, FavoriteComicsAdapter.VH>(FavoriteComicItemDiffUtilItemCallback) {
  private val dateFormatter = SimpleDateFormat("hh:mm, dd/MM/yyyy", Locale.getDefault())

  private val _clickDelete = PublishRelay.create<ComicItem>()
  val clickDelete get() = _clickDelete.asObservable()

  private val _clickItem = PublishRelay.create<ComicItem>()
  val clickItem get() = _clickItem.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate R.layout.item_recycler_favorite_comics, parent)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View, parent: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val swipeRevealLayout = itemView.swipe_reveal_layout!!

    private val textComicName = itemView.text_comic_name!!
    private val textCreatedAt = itemView.text_created_at!!
    private val textViewCount = itemView.text_view!!

    private fun <T> Observable<T>.getItemAtPosition(): Observable<ComicItem> {
      return map { adapterPosition }
        .filter { it != NO_POSITION }
        .map { getItem(it) }
    }

    init {
      itemView
        .text_delete
        .clicks()
        .takeUntil(parent.detaches())
        .getItemAtPosition()
        .subscribe(_clickDelete)
        .addTo(compositeDisposable)

      itemView
        .cardView
        .clicks()
        .takeUntil(parent.detaches())
        .getItemAtPosition()
        .subscribe(_clickItem)
        .addTo(compositeDisposable)
    }

    fun bind(comic: ComicItem) {
      viewBinderHelper.bind(swipeRevealLayout, comic.url)

      glide
        .load(comic.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)

      textComicName.text = comic.title
      textCreatedAt.text = "Added at: ${comic.createdAt?.let { dateFormatter.format(it) } ?: "N/A"}"
      textViewCount.text = comic.view
    }
  }
}
