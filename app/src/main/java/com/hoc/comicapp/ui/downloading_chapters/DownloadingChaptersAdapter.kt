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
import kotlinx.android.synthetic.main.item_recycler_downloading_chapter.view.*
import timber.log.Timber

object DownloadingChapterItemDiffUtilItemCallback : DiffUtil.ItemCallback<Chapter>() {
  override fun areItemsTheSame(oldItem: Chapter, newItem: Chapter) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: Chapter, newItem: Chapter) = oldItem == newItem
  override fun getChangePayload(oldItem: Chapter, newItem: Chapter): Any? {
    return if (newItem.isSameExceptProgress(oldItem)) {
      newItem.progress
    } else {
      null
    }
  }
}

class DownloadingChaptersAdapter(
  private val glide: GlideRequests
) : ListAdapter<Chapter, DownloadingChaptersAdapter.VH>(DownloadingChapterItemDiffUtilItemCallback) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate R.layout.item_recycler_downloading_chapter)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
    val progress = payloads.firstOrNull() as? Int? ?: return onBindViewHolder(holder, position)
    Timber.d("onBindViewHolder progress=$progress")
    holder.updateProgress(progress)
  }

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textChapterTitle = itemView.text_chapter_title!!
    private val textComicTitle = itemView.text_comic_title!!
    private val progress = itemView.progress!!
    private val textProgress = itemView.text_progress!!
    private val imageCancelDownload = itemView.image_cancel_download!!

    init {
      imageCancelDownload.setOnClickListener {
        //TODO
      }
    }

    fun bind(chapter: Chapter) {
      textChapterTitle.text = chapter.title
      textComicTitle.text = chapter.comicTitle
      updateProgress(chapter.progress)
    }

    fun updateProgress(progress: Int) {
      this.progress.progress = progress
      textProgress.text = "$progress% "
    }
  }
}