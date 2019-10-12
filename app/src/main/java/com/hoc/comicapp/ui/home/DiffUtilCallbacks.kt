package com.hoc.comicapp.ui.home

import androidx.recyclerview.widget.DiffUtil
import com.hoc.comicapp.domain.models.Comic

object HomeListItemDiffUtilItemCallback : DiffUtil.ItemCallback<HomeListItem>() {
  override fun areItemsTheSame(oldItem: HomeListItem, newItem: HomeListItem) = when {
    oldItem is HomeListItem.NewestListState && newItem is HomeListItem.NewestListState -> true
    oldItem is HomeListItem.MostViewedListState && newItem is HomeListItem.MostViewedListState -> true
    oldItem is HomeListItem.UpdatedItem.ComicItem && newItem is HomeListItem.UpdatedItem.ComicItem -> oldItem.comic.link == newItem.comic.link
    oldItem is HomeListItem.UpdatedItem.Error && newItem is HomeListItem.UpdatedItem.Error -> true
    oldItem is HomeListItem.UpdatedItem.Loading && newItem is HomeListItem.UpdatedItem.Loading -> true
    oldItem is HomeListItem.Header && newItem is HomeListItem.Header -> oldItem.type == newItem.type
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