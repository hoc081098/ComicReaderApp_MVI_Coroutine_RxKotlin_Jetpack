package com.hoc.comicapp.ui.category_detail

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.PopularItem
import com.hoc.comicapp.ui.detail.ComicArg
import com.hoc.comicapp.utils.inflate
import kotlinx.android.synthetic.main.item_recycler_category_detail_popular_comic.view.*

private object PopularItemDiffCallback : DiffUtil.ItemCallback<PopularItem>() {
  override fun areItemsTheSame(oldItem: PopularItem, newItem: PopularItem) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: PopularItem, newItem: PopularItem) = oldItem == newItem
}

class PopularHorizontalAdapter(
  private val glide: GlideRequests,
  private val onClickComic: (ComicArg) -> Unit
) :
  ListAdapter<PopularItem, PopularHorizontalAdapter.VH>(PopularItemDiffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate R.layout.item_recycler_category_detail_popular_comic)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!

    init {
      itemView.setOnClickListener {
        val position = adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val item = getItem(position)
          onClickComic(
            ComicArg(
              title = item.title,
              thumbnail = item.thumbnail,
              link = item.link,
              view = ""
            )
          )
        }
      }
    }

    fun bind(item: PopularItem) {
      glide
        .load(item.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)

      textComicName.text = item.title
      textChapter.text = item.lastChapter.chapterName
    }
  }
}
