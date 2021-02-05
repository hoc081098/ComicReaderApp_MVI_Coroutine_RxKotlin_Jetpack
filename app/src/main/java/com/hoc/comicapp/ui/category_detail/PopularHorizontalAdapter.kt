package com.hoc.comicapp.ui.category_detail

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.databinding.ItemRecyclerCategoryDetailPopularComicBinding
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.PopularItem
import com.hoc.comicapp.utils.inflater

private object PopularItemDiffCallback : DiffUtil.ItemCallback<PopularItem>() {
  override fun areItemsTheSame(oldItem: PopularItem, newItem: PopularItem) =
    oldItem.link == newItem.link

  override fun areContentsTheSame(oldItem: PopularItem, newItem: PopularItem) = oldItem == newItem
}

class PopularHorizontalAdapter(
  private val glide: GlideRequests,
  private val onClickComic: (Arguments.ComicDetailArgs) -> Unit,
) :
  ListAdapter<PopularItem, PopularHorizontalAdapter.VH>(PopularItemDiffCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(
      ItemRecyclerCategoryDetailPopularComicBinding.inflate(
        parent.inflater,
        parent,
        false
      )
    )

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(private val binding: ItemRecyclerCategoryDetailPopularComicBinding) :
    RecyclerView.ViewHolder(binding.root) {
    init {
      itemView.setOnClickListener {
        val position = bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val item = getItem(position)
          onClickComic(
            Arguments.ComicDetailArgs(
              title = item.title,
              thumbnail = item.thumbnail,
              link = item.link,
              view = "",
              remoteThumbnail = item.thumbnail
            )
          )
        }
      }
    }

    fun bind(item: PopularItem) = binding.run {
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
