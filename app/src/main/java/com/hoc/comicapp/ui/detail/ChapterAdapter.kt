package com.hoc.comicapp.ui.detail

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import antonkozyriatskyi.circularprogressindicator.CircularProgressIndicator
import com.google.android.material.chip.Chip
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.ItemRecyclerChapterBinding
import com.hoc.comicapp.databinding.ItemRecyclerDetailBinding
import com.hoc.comicapp.ui.detail.ComicDetailViewState.Category
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloaded
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloading
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Loading
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.NotYetDownload
import com.hoc.comicapp.utils.inflate
import com.hoc.comicapp.utils.inflater
import timber.log.Timber

sealed class ChapterAdapterItem {
  data class Header(
    val shortenedContent: String,
    val categories: List<Category>,
  ) : ChapterAdapterItem()

  data class Chapter(val chapter: ComicDetailViewState.Chapter) : ChapterAdapterItem()

  object Dummy : ChapterAdapterItem()
}

object ChapterDiffUtilItemCallback : DiffUtil.ItemCallback<ChapterAdapterItem>() {
  override fun areItemsTheSame(oldItem: ChapterAdapterItem, newItem: ChapterAdapterItem) = when {
    oldItem is ChapterAdapterItem.Header && newItem is ChapterAdapterItem.Header -> true
    oldItem is ChapterAdapterItem.Chapter && newItem is ChapterAdapterItem.Chapter -> oldItem.chapter.chapterLink == newItem.chapter.chapterLink
    oldItem is ChapterAdapterItem.Dummy && newItem is ChapterAdapterItem.Dummy -> true
    else -> oldItem == newItem
  }

  override fun areContentsTheSame(oldItem: ChapterAdapterItem, newItem: ChapterAdapterItem) =
    oldItem == newItem

  override fun getChangePayload(oldItem: ChapterAdapterItem, newItem: ChapterAdapterItem): Any? {
    return when {
      oldItem is ChapterAdapterItem.Chapter &&
        newItem is ChapterAdapterItem.Chapter &&
        newItem.chapter.isSameExceptDownloadState(oldItem.chapter) -> newItem.chapter.downloadState
      else -> null
    }
  }
}

class ChapterAdapter(
  private val onClickReadButton: (readFirst: Boolean) -> Unit,
  private val onClickChapter: (ComicDetailViewState.Chapter, View) -> Unit,
  private val onClickChapterChip: (Category) -> Unit,
) :
  ListAdapter<ChapterAdapterItem, ChapterAdapter.VH>(ChapterDiffUtilItemCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    val view = parent inflate viewType
    return when (viewType) {
      R.layout.item_recycler_detail -> HeaderVH(
        ItemRecyclerDetailBinding.inflate(
          parent.inflater,
          parent,
          false
        )
      )
      R.layout.item_recycler_chapter -> ChapterVH(
        ItemRecyclerChapterBinding.inflate(
          parent.inflater,
          parent,
          false
        )
      )
      R.layout.item_recycler_chapter_dummy -> DummyVH(view)
      else -> error("Unknown viewType=$viewType")
    }
  }

  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is ChapterAdapterItem.Header -> R.layout.item_recycler_detail
      is ChapterAdapterItem.Chapter -> R.layout.item_recycler_chapter
      ChapterAdapterItem.Dummy -> R.layout.item_recycler_chapter_dummy
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
    if (payloads.isEmpty()) {
      return onBindViewHolder(holder, position)
    }

    payloads.forEach {
      if (it is ComicDetailViewState.DownloadState && holder is ChapterVH) {
        Timber.d("Bind...$it")
        holder.updateDownloadState(it)
      }
    }
  }

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: ChapterAdapterItem)
  }

  private inner class ChapterVH(private val binding: ItemRecyclerChapterBinding) :
    ChapterAdapter.VH(binding.root),
    View.OnClickListener {
    init {
      binding.circularProgress.run {
        maxProgress = 100.0
        setProgressTextAdapter(PROGRESS_TEXT_ADAPTER)
      }

      itemView.setOnClickListener(this)
      binding.imageDownload.setOnClickListener(this)
    }

    override fun onClick(v: View) {
      val position = bindingAdapterPosition
      if (position == NO_POSITION) return

      val item = getItem(position) as? ChapterAdapterItem.Chapter ?: return
      onClickChapter(item.chapter, v)
    }

    override fun bind(item: ChapterAdapterItem) = binding.run {
      if (item !is ChapterAdapterItem.Chapter) return
      val chapter = item.chapter
      textChapterTitle.text = chapter.chapterName
      textChapterTime.text = chapter.time
      textChapterView.text = "${chapter.view} "
      updateDownloadState(chapter.downloadState)
    }

    fun updateDownloadState(downloadState: ComicDetailViewState.DownloadState) = binding.run {
      when (downloadState) {
        Downloaded -> {
          imageDownload.setImageResource(R.drawable.ic_done_accent_24dp)
          circularProgress.isInvisible = true
        }
        is Downloading -> {
          imageDownload.setImageDrawable(null)
          circularProgress.isInvisible = false
          circularProgress.setCurrentProgress(downloadState.progress.toDouble())
        }
        NotYetDownload -> {
          imageDownload.setImageResource(R.drawable.ic_file_download_white_24dp)
          circularProgress.isInvisible = true
        }
        Loading -> {
          imageDownload.setImageDrawable(null)
          circularProgress.isInvisible = true
        }
      }
    }
  }

  private inner class HeaderVH(private val binding: ItemRecyclerDetailBinding) :
    VH(binding.root),
    View.OnClickListener {

    init {
      binding.buttonReadLatestChapter.setOnClickListener(this)
      binding.buttonReadFirstChapter.setOnClickListener(this)
    }

    override fun onClick(v: View) = onClickReadButton(v.id == R.id.button_read_first_chapter)

    override fun bind(item: ChapterAdapterItem) = binding.run {
      if (item !is ChapterAdapterItem.Header) return

      textShortendedContent.text = item.shortenedContent

      categoriesGroup.removeAllViews()
      item.categories
        .map { category ->
          Chip(itemView.context).apply {
            text = category.name
            isCheckable = false
            isClickable = true
            setOnClickListener {
              onClickChapterChip(category)
            }
          }
        }
        .forEach(categoriesGroup::addView)
    }
  }

  private class DummyVH(itemView: View) : VH(itemView) {
    override fun bind(item: ChapterAdapterItem) = Unit
  }

  private companion object {
    val PROGRESS_TEXT_ADAPTER = CircularProgressIndicator.ProgressTextAdapter { "${it.toInt()}%" }
  }
}
