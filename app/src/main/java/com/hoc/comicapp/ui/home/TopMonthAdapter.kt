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
import kotlinx.android.synthetic.main.item_recyclerview_top_month_comic_or_recommened.view.*

class TopMonthAdapter : ListAdapter<Comic, TopMonthAdapter.VH>(ComicDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return LayoutInflater.from(parent.context)
      .inflate(R.layout.item_recyclerview_top_month_comic_or_recommened, parent, false)
      .let(::VH)
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!
    private val textView = itemView.text_view_or_last_updated_time!!

    fun bind(item: Comic) {
      textComicName.text = item.title
      textChapter.text = item.chapters.first().chapterName
      textView.text = item.view

      GlideApp.with(itemView.context)
        .load(item.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)
    }
  }
}