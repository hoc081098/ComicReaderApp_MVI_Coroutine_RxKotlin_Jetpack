package com.hoc.comicapp.ui.downloading_chapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState.Chapter
import com.hoc.comicapp.utils.inflate
import kotlinx.android.synthetic.main.item_recycler_chapter.view.*
import java.text.SimpleDateFormat
import java.util.*

object DownloadingChapterItemDiffUtilItemCallback : DiffUtil.ItemCallback<Chapter>() {
  override fun areItemsTheSame(oldItem: Chapter, newItem: Chapter) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: Chapter, newItem: Chapter) = oldItem == newItem
}

class DownloadingChaptersAdapter(
  private val glide: GlideRequests
) : ListAdapter<Chapter, DownloadingChaptersAdapter.VH>(DownloadingChapterItemDiffUtilItemCallback) {
  private val dateFormatter = SimpleDateFormat("hh:mm, dd/MM/yyyy", Locale.getDefault())

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate R.layout.item_recycler_chapter)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(chapter: Chapter) {
      itemView.text_chapter_title.text = "${chapter.progress}%"
    }
  }
}