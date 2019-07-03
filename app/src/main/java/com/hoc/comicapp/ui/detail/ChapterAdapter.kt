package com.hoc.comicapp.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.google.android.material.chip.Chip
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.ComicDetail
import com.hoc.comicapp.domain.models.ComicDetail.Chapter
import com.hoc.comicapp.utils.toast
import kotlinx.android.synthetic.main.item_recycler_chapter.view.*
import kotlinx.android.synthetic.main.item_recycler_detail.view.*

sealed class ChapterItem {
  data class Header(
    val shortenedContent: String,
    val categories: List<ComicDetail.Category>
  ) : ChapterItem()

  data class Chapter(val chapter: ComicDetail.Chapter) : ChapterItem()
}

object ChapterDiffUtilItemCallback : DiffUtil.ItemCallback<ChapterItem>() {
  override fun areItemsTheSame(oldItem: ChapterItem, newItem: ChapterItem) = when {
    oldItem is ChapterItem.Header && newItem is ChapterItem.Header -> true
    oldItem is ChapterItem.Chapter && newItem is ChapterItem.Chapter -> oldItem.chapter.chapterLink == newItem.chapter.chapterLink
    else -> oldItem == newItem
  }

  override fun areContentsTheSame(oldItem: ChapterItem, newItem: ChapterItem) = oldItem == newItem
}

class ChapterAdapter(private val onClickChapter: (Chapter) -> Unit) :
  ListAdapter<ChapterItem, ChapterAdapter.VH>(ChapterDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
    return when (viewType) {
      R.layout.item_recycler_detail -> HeaderVH(view)
      R.layout.item_recycler_chapter -> ChapterVH(view)
      else -> error("Unknown viewType=$viewType")
    }
  }

  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is ChapterItem.Header -> R.layout.item_recycler_detail
      is ChapterItem.Chapter -> R.layout.item_recycler_chapter
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: ChapterItem)
  }

  private inner class ChapterVH(itemView: View) : ChapterAdapter.VH(itemView) {
    private val textChapterTitle = itemView.text_chapter_title!!
    private val textChapterTime = itemView.text_chapter_time!!
    private val textChapterView = itemView.text_chapter_view!!

    init {
      itemView.setOnClickListener {
        val position = adapterPosition
        if (position != NO_POSITION) {
          val item = getItem(position)
          if (item is ChapterItem.Chapter) {
            onClickChapter(item.chapter)
          }
        }
      }
    }

    override fun bind(item: ChapterItem) {
      if (item !is ChapterItem.Chapter) return
      val chapter = item.chapter
      textChapterTitle.text = chapter.chapterName
      textChapterTime.text = chapter.time
      textChapterView.text = chapter.view
    }
  }

  private class HeaderVH(itemView: View) : VH(itemView) {
    private val textShortendedContent = itemView.text_shortended_content!!
    private val categoriesGroup = itemView.categories_group!!

    override fun bind(item: ChapterItem) {
      if (item !is ChapterItem.Header) return

      textShortendedContent.text = item.shortenedContent

      categoriesGroup.removeAllViews()
      item.categories
        .map { category ->
          Chip(itemView.context).apply {
            text = category.name
            isCheckable = false
            isClickable = true
            setOnClickListener { context.toast("Click ${category.link}") }
          }
        }
        .forEach(categoriesGroup::addView)
    }
  }
}