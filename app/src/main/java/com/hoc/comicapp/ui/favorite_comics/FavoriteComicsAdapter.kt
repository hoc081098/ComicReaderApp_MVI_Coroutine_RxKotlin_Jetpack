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
import com.hoc.comicapp.databinding.ItemRecyclerFavoriteComicsBinding
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ComicItem
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflater
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.detaches
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import java.text.SimpleDateFormat
import java.util.Locale

object FavoriteComicItemDiffUtilItemCallback : DiffUtil.ItemCallback<ComicItem>() {
  override fun areItemsTheSame(oldItem: ComicItem, newItem: ComicItem) = oldItem.url == newItem.url
  override fun areContentsTheSame(oldItem: ComicItem, newItem: ComicItem) = oldItem == newItem
}

class FavoriteComicsAdapter(
  private val glide: GlideRequests,
  private val viewBinderHelper: ViewBinderHelper,
  private val compositeDisposable: CompositeDisposable,
) :
  ListAdapter<ComicItem, FavoriteComicsAdapter.VH>(FavoriteComicItemDiffUtilItemCallback) {
  private val dateFormatter = SimpleDateFormat("hh:mm, dd/MM/yyyy", Locale.getDefault())

  private val _clickDelete = PublishRelay.create<ComicItem>()
  val clickDelete get() = _clickDelete.asObservable()

  private val _clickItem = PublishRelay.create<ComicItem>()
  val clickItem get() = _clickItem.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(ItemRecyclerFavoriteComicsBinding.inflate(parent.inflater, parent, false), parent)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(private val binding: ItemRecyclerFavoriteComicsBinding, parent: View) :
    RecyclerView.ViewHolder(binding.root) {

    private fun <T : Any> Observable<T>.getItemAtPosition(): Observable<ComicItem> {
      return map { bindingAdapterPosition }
        .filter { it != NO_POSITION }
        .map(::getItem)
    }

    init {
      binding
        .textDelete
        .clicks()
        .takeUntil(parent.detaches())
        .getItemAtPosition()
        .subscribe(_clickDelete)
        .addTo(compositeDisposable)

      binding
        .cardView
        .clicks()
        .takeUntil(parent.detaches())
        .getItemAtPosition()
        .subscribe(_clickItem)
        .addTo(compositeDisposable)
    }

    fun bind(comic: ComicItem) = binding.run {
      viewBinderHelper.bind(swipeRevealLayout, comic.url)

      glide
        .load(comic.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)

      textComicName.text = comic.title
      textCreatedAt.text = "Added at: ${comic.createdAt?.let { dateFormatter.format(it) } ?: "N/A"}"
      textView.text = comic.view
    }
  }
}
