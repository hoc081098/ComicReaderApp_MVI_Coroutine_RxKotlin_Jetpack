package com.hoc.comicapp.ui.detail

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.google.android.material.chip.Chip
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.detail.ComicDetailViewState.Category
import com.hoc.comicapp.utils.inflate
import com.hoc.comicapp.utils.toast
import kotlinx.android.synthetic.main.item_recycler_chapter.view.*
import kotlinx.android.synthetic.main.item_recycler_detail.view.*

sealed class ChapterAdapterItem {
  data class Header(
    val shortenedContent: String,
    val categories: List<Category>
  ) : ChapterAdapterItem()

  data class Chapter(val chapter: ComicDetailViewState.Chapter) : ChapterAdapterItem()
}

object ChapterDiffUtilItemCallback : DiffUtil.ItemCallback<ChapterAdapterItem>() {
  override fun areItemsTheSame(oldItem: ChapterAdapterItem, newItem: ChapterAdapterItem) = when {
    oldItem is ChapterAdapterItem.Header && newItem is ChapterAdapterItem.Header -> true
    oldItem is ChapterAdapterItem.Chapter && newItem is ChapterAdapterItem.Chapter -> oldItem.chapter.chapterLink == newItem.chapter.chapterLink
    else -> oldItem == newItem
  }

  override fun areContentsTheSame(oldItem: ChapterAdapterItem, newItem: ChapterAdapterItem) = oldItem == newItem
}

class ChapterAdapter(
  private val onClickChapter: (ComicDetailViewState.Chapter) -> Unit,
  private val onClickReadButton: (readFirst: Boolean) -> Unit,
  private val onClickDownload: (ComicDetailViewState.Chapter) -> Unit
) :
  ListAdapter<ChapterAdapterItem, ChapterAdapter.VH>(ChapterDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    val view = parent inflate viewType
    return when (viewType) {
      R.layout.item_recycler_detail -> HeaderVH(view)
      R.layout.item_recycler_chapter -> ChapterVH(view)
      else -> error("Unknown viewType=$viewType")
    }
  }

  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is ChapterAdapterItem.Header -> R.layout.item_recycler_detail
      is ChapterAdapterItem.Chapter -> R.layout.item_recycler_chapter
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: ChapterAdapterItem)
  }

  private inner class ChapterVH(itemView: View) : ChapterAdapter.VH(itemView), View.OnClickListener {
    private val textChapterTitle = itemView.text_chapter_title!!
    private val textChapterTime = itemView.text_chapter_time!!
    private val textChapterView = itemView.text_chapter_view!!
    private val imageDownload = itemView.image_download!!

    init {
      itemView.setOnClickListener(this)
      imageDownload.setOnClickListener(this)
    }

    override fun onClick(v: View) {
      val position = adapterPosition
      if (position == NO_POSITION) return
      val item = getItem(position) as? ChapterAdapterItem.Chapter ?: return

      when {
        v.id == R.id.image_download -> onClickDownload(item.chapter)
        else -> onClickChapter(item.chapter)
      }
    }


    override fun bind(item: ChapterAdapterItem) {
      if (item !is ChapterAdapterItem.Chapter) return
      val chapter = item.chapter
      textChapterTitle.text = chapter.chapterName
      textChapterTime.text = chapter.time
      textChapterView.text = chapter.view
    }
  }

  private inner class HeaderVH(itemView: View) : VH(itemView), View.OnClickListener {
    private val textShortendedContent = itemView.text_shortended_content!!
    private val categoriesGroup = itemView.categories_group!!

    init {
      itemView.button_read_latest_chapter.setOnClickListener(this)
      itemView.button_read_first_chapter.setOnClickListener(this)
    }

    override fun onClick(v: View) = onClickReadButton(v.id == R.id.button_read_first_chapter)


    override fun bind(item: ChapterAdapterItem) {
      if (item !is ChapterAdapterItem.Header) return

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