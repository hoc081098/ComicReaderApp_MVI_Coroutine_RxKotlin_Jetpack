package com.hoc.comicapp.ui.downloading_chapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.databinding.ItemRecyclerDownloadingChapterBinding
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState.Chapter
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflater
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.detaches
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
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
  private val compositeDisposable: CompositeDisposable,
) :
  ListAdapter<Chapter, DownloadingChaptersAdapter.VH>(DownloadingChapterItemDiffUtilItemCallback) {
  private val _clickCancel = PublishRelay.create<Chapter>()
  val clickCancel get() = _clickCancel.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(ItemRecyclerDownloadingChapterBinding.inflate(parent.inflater, parent, false), parent)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
    if (payloads.isEmpty()) {
      return onBindViewHolder(holder, position)
    }

    payloads.forEach {
      if (it is Int) {
        Timber.d("onBindViewHolder progress=$it")
        holder.updateProgress(it)
      }
    }
  }

  inner class VH(private val binding: ItemRecyclerDownloadingChapterBinding, parent: ViewGroup) :
    RecyclerView.ViewHolder(binding.root) {
    init {
      binding.imageCancelDownload
        .clicks()
        .takeUntil(parent.detaches())
        .map { bindingAdapterPosition }
        .map { getItem(it) }
        .subscribe(_clickCancel)
        .addTo(compositeDisposable)
    }

    fun bind(chapter: Chapter) = binding.run {
      textChapterTitle.text = chapter.title
      textComicTitle.text = chapter.comicTitle
      updateProgress(chapter.progress)
    }

    fun updateProgress(progress: Int) = binding.run {
      this.progress.progress = progress
      textProgress.text = "$progress% "
    }
  }
}
