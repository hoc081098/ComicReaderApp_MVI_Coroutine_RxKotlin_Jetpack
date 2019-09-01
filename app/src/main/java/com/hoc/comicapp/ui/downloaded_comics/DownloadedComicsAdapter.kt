package com.hoc.comicapp.ui.downloaded_comics

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem

object DownloadedComicItemDiffUtilItemCallback : DiffUtil.ItemCallback<ComicItem>() {
  override fun areItemsTheSame(oldItem: ComicItem, newItem: ComicItem): Boolean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun areContentsTheSame(oldItem: ComicItem, newItem: ComicItem): Boolean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}

class DownloadedComicsAdapter :
  ListAdapter<ComicItem, DownloadedComicsAdapter.VH>(DownloadedComicItemDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
  }

  override fun onBindViewHolder(holder: VH, position: Int) {
  }

  class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {

  }
}