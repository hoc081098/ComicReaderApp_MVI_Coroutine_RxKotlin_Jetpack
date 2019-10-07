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
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.item_recycler_downloaded_comics.view.*
import java.text.SimpleDateFormat
import java.util.*

object DownloadedComicItemDiffUtilItemCallback : DiffUtil.ItemCallback<ComicItem>() {
  override fun areItemsTheSame(oldItem: ComicItem, newItem: ComicItem) = oldItem.comicLink == newItem.comicLink
  override fun areContentsTheSame(oldItem: ComicItem, newItem: ComicItem) = oldItem == newItem
}

class DownloadedComicsAdapter(
  private val glide: GlideRequests,
  private val viewBinderHelper: ViewBinderHelper,
  private val compositeDisposable: CompositeDisposable
) :
  ListAdapter<ComicItem, DownloadedComicsAdapter.VH>(DownloadedComicItemDiffUtilItemCallback) {
  private val dateFormatter = SimpleDateFormat("hh:mm, dd/MM/yyyy", Locale.getDefault())

  private val _clickDelete = PublishRelay.create<ComicItem>()
  val clickDelete get() = _clickDelete.asObservable()

  private val _clickItem = PublishRelay.create<ComicItem>()
  val clickItem get() = _clickItem.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate R.layout.item_recycler_downloaded_comics, parent)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View, parent: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val swipeRevealLayout = itemView.swipe_reveal_layout!!

    private val textComicName = itemView.text_comic_name!!
    private val textView = itemView.text_view!!

    private val textChapterName3 = itemView.text_chapter_name_3!!
    private val textChapterTime3 = itemView.text_chapter_time_3!!

    private val textChapterName2 = itemView.text_chapter_name_2!!
    private val textChapterTime2 = itemView.text_chapter_time_2!!

    private val textChapterName1 = itemView.text_chapter_name_1!!
    private val textChapterTime1 = itemView.text_chapter_time_1!!

    private val textChapters = listOf(
      textChapterName1 to textChapterTime1,
      textChapterName2 to textChapterTime2,
      textChapterName3 to textChapterTime3
    )


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
      viewBinderHelper.bind(swipeRevealLayout ,comic.comicLink)

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
