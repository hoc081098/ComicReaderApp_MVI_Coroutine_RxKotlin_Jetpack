package com.hoc.comicapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.utils.asObservable
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import kotlinx.android.synthetic.main.item_recyclerview_top_month_comic_or_recommened.view.*

class SuggestAdapter : ListAdapter<Comic, SuggestAdapter.VH>(ComicDiffUtilItemCallback) {
  private val clickComicS = PublishRelay.create<Comic>()
  val clickComicObservable get() = clickComicS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return LayoutInflater.from(parent.context)
      .inflate(R.layout.item_recyclerview_top_month_comic_or_recommened, parent, false)
      .let { VH(it, parent) }
  }

  override fun onBindViewHolder(holder: VH, position: Int) =
    holder.bind(getItem(position % itemCount))

  inner class VH(itemView: View, parent: ViewGroup) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!
    private val textLastUpdatedTime = itemView.text_view_or_last_updated_time!!
    private val imageIconClock = itemView.image_eye_or_clock!!

    init {
      itemView
        .clicks()
        .takeUntil(parent.detaches())
        .map { adapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .map { getItem(it) }
        .subscribe(clickComicS::accept)
    }

    fun bind(item: Comic) {
      textComicName.text = item.title
      textChapter.text = item.chapters.first().chapterName
      textLastUpdatedTime.text = item.chapters.first().time
      imageIconClock.setImageResource(R.drawable.ic_access_time_white_24dp)

      GlideApp.with(itemView.context)
        .load(item.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)
    }
  }
}