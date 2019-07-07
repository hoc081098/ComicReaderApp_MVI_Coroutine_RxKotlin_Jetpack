package com.hoc.comicapp.ui.chapter_detail

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.inflate
import kotlinx.android.synthetic.main.item_recycler_chapter_detail_image.view.*

object StringDiffUtilItemCallback : DiffUtil.ItemCallback<String>() {
  override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
  override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}

class ChapterImageAdapter(
  private val glide: GlideRequests
) :
  ListAdapter<String, ChapterImageAdapter.VH>(StringDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate R.layout.item_recycler_chapter_detail_image)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageChapter = itemView.image_chapter!!

    fun bind(imageUrl: String) {
      glide
        .load(imageUrl)
        .fitCenter()
        .into(imageChapter)
    }
  }
}