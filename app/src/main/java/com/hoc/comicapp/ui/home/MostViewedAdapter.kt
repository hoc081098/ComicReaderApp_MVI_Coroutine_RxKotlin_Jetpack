package com.hoc.comicapp.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.ui.home.HomeAdapter.Companion.MOST_VIEW_COMIC_ITEM_VIEW_TYPE
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.item_recyclerview_top_month_comic_or_recommened.view.*

class MostViewedAdapter(
  private val glide: GlideRequests,
  private val compositeDisposable: CompositeDisposable
) :
  ListAdapter<Comic, MostViewedAdapter.VH>(MostViewedComicDiffUtilItemCallback) {
  private val clickComicS = PublishRelay.create<Comic>()
  val clickComicObservable get() = clickComicS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    when (viewType) {
      MOST_VIEW_COMIC_ITEM_VIEW_TYPE -> return VH(parent inflate R.layout.item_recyclerview_top_month_comic_or_recommened)
      else -> throw IllegalStateException("viewType must be $MOST_VIEW_COMIC_ITEM_VIEW_TYPE, but viewType=$viewType")
    }
  }

  override fun getItemViewType(position: Int) = MOST_VIEW_COMIC_ITEM_VIEW_TYPE

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!
    private val textView = itemView.text_view_or_last_updated_time!!

    init {
      itemView
        .clicks()
        .map { adapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .map { getItem(it) }
        .subscribe(clickComicS)
        .addTo(compositeDisposable)
    }

    fun bind(item: Comic) {
      textComicName.text = item.title
      textChapter.text = item.lastChapters.lastOrNull()?.chapterName
      textView.text = item.view

      glide
        .load(item.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)
    }
  }
}