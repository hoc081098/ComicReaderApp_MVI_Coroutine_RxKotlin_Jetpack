package com.hoc.comicapp.ui.downloaded_comics

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.databinding.ItemRecyclerDownloadedComicsBinding
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
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

object DownloadedComicItemDiffUtilItemCallback : DiffUtil.ItemCallback<ComicItem>() {
  override fun areItemsTheSame(oldItem: ComicItem, newItem: ComicItem) =
    oldItem.comicLink == newItem.comicLink

  override fun areContentsTheSame(oldItem: ComicItem, newItem: ComicItem) = oldItem == newItem
}

class DownloadedComicsAdapter(
  private val glide: GlideRequests,
  private val viewBinderHelper: ViewBinderHelper,
  private val compositeDisposable: CompositeDisposable,
) :
  ListAdapter<ComicItem, DownloadedComicsAdapter.VH>(DownloadedComicItemDiffUtilItemCallback) {
  private val dateFormatter = SimpleDateFormat("hh:mm, dd/MM/yyyy", Locale.getDefault())

  private val _clickDelete = PublishRelay.create<ComicItem>()
  val clickDelete get() = _clickDelete.asObservable()

  private val _clickItem = PublishRelay.create<ComicItem>()
  val clickItem get() = _clickItem.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(ItemRecyclerDownloadedComicsBinding.inflate(parent.inflater, parent, false), parent)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(private val binding: ItemRecyclerDownloadedComicsBinding, parent: View) :
    RecyclerView.ViewHolder(binding.root) {
    private val textChapters = binding.run {
      arrayOf(
        textChapterName1 to textChapterTime1,
        textChapterName2 to textChapterTime2,
        textChapterName3 to textChapterTime3
      )
    }

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
      viewBinderHelper.bind(swipeRevealLayout, comic.comicLink)

      glide
        .load(comic.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)

      textComicName.text = comic.title
      textView.text = comic.view

      textChapters
        .zip(comic.chapters)
        .forEach { (textViews, chapter) ->
          textViews.first.text = chapter.chapterName
          textViews.second.text = dateFormatter.format(chapter.downloadedAt)
        }
    }
  }
}
