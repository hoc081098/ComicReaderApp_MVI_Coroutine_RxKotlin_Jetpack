package com.hoc.comicapp.ui.home

import androidx.recyclerview.widget.DiffUtil
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.ui.home.HomeListItem.Header
import com.hoc.comicapp.ui.home.HomeListItem.MostViewedListState
import com.hoc.comicapp.ui.home.HomeListItem.NewestListState
import com.hoc.comicapp.ui.home.HomeListItem.UpdatedItem

object HomeListItemDiffUtilItemCallback : DiffUtil.ItemCallback<HomeListItem>() {
  override fun areItemsTheSame(oldItem: HomeListItem, newItem: HomeListItem) = when {
    oldItem is NewestListState && newItem is NewestListState -> true
    oldItem is MostViewedListState && newItem is MostViewedListState -> true
    oldItem is UpdatedItem.ComicItem && newItem is UpdatedItem.ComicItem -> oldItem.comic.link == newItem.comic.link
    oldItem is UpdatedItem.Error && newItem is UpdatedItem.Error -> true
    oldItem is UpdatedItem.Loading && newItem is UpdatedItem.Loading -> true
    oldItem is Header && newItem is Header -> oldItem.type == newItem.type
    else -> oldItem == newItem
  }

  override fun areContentsTheSame(oldItem: HomeListItem, newItem: HomeListItem) = oldItem == newItem
}

object NewestComicDiffUtilItemCallback : DiffUtil.ItemCallback<Comic>() {
  override fun areItemsTheSame(oldItem: Comic, newItem: Comic) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: Comic, newItem: Comic) = oldItem == newItem
}

object MostViewedComicDiffUtilItemCallback : DiffUtil.ItemCallback<Comic>() {
  override fun areItemsTheSame(oldItem: Comic, newItem: Comic) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: Comic, newItem: Comic) = oldItem == newItem
}
