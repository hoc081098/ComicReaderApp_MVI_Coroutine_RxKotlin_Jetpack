package com.hoc.comicapp.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.hoc.comicapp.R
import com.hoc.domain.models.ComicDetail.Chapter
import kotlinx.android.synthetic.main.item_recycler_chapter.view.*

object ChapterDiffUtilItemCallback : DiffUtil.ItemCallback<Chapter>() {
  override fun areItemsTheSame(oldItem: Chapter, newItem: Chapter) = oldItem.chapterLink == newItem.chapterLink
  override fun areContentsTheSame(oldItem: Chapter, newItem: Chapter) = oldItem == newItem
}

class ChapterAdapter(private val onClickChapter: (Chapter) -> Unit) :
  ListAdapter<Chapter, ChapterAdapter.VH>(ChapterDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    LayoutInflater.from(parent.context)
      .inflate(R.layout.item_recycler_chapter, parent, false)
      .let(::VH)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))


  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textChapterTitle = itemView.text_chapter_title!!
    private val textChapterTime = itemView.text_chapter_time!!
    private val textChapterView = itemView.text_chapter_view!!

    init {
      itemView.setOnClickListener {
        val position = adapterPosition
        if (position != NO_POSITION) {
          onClickChapter(getItem(position))
        }
      }
    }

    fun bind(item: Chapter) {
      textChapterTitle.text = item.chapterName
      textChapterTime.text = item.time
      textChapterView.text = item.view
    }
  }
}