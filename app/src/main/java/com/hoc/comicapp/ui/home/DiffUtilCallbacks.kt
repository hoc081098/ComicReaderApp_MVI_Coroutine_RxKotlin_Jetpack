package com.hoc.comicapp.ui.home

import androidx.recyclerview.widget.DiffUtil
import com.hoc.comicapp.domain.models.SuggestComic
import com.hoc.comicapp.domain.models.TopMonthComic

object HomeListItemDiffUtilItemCallback : DiffUtil.ItemCallback<HomeListItem>() {
  override fun areItemsTheSame(oldItem: HomeListItem, newItem: HomeListItem) = when {
    oldItem is HomeListItem.SuggestListState && newItem is HomeListItem.SuggestListState -> oldItem == newItem
    oldItem is HomeListItem.TopMonthListState && newItem is HomeListItem.TopMonthListState -> oldItem == newItem
    oldItem is HomeListItem.UpdatedItem.ComicItem && newItem is HomeListItem.UpdatedItem.ComicItem -> oldItem.comic.link == newItem.comic.link
    oldItem is HomeListItem.UpdatedItem.Error && newItem is HomeListItem.UpdatedItem.Error -> oldItem.errorMessage == newItem.errorMessage
    oldItem is HomeListItem.UpdatedItem.Loading && newItem is HomeListItem.UpdatedItem.Loading -> true
    oldItem is HomeListItem.Header && newItem is HomeListItem.Header -> oldItem.type == newItem.type
    else -> oldItem == newItem
  }

  override fun areContentsTheSame(oldItem: HomeListItem, newItem: HomeListItem) = oldItem == newItem
}

object SuggestComicDiffUtilItemCallback : DiffUtil.ItemCallback<SuggestComic>() {
  override fun areItemsTheSame(oldItem: SuggestComic, newItem: SuggestComic) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: SuggestComic, newItem: SuggestComic) = oldItem == newItem
}

object TopMonthComicDiffUtilItemCallback : DiffUtil.ItemCallback<TopMonthComic>() {
  override fun areItemsTheSame(oldItem: TopMonthComic, newItem: TopMonthComic) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: TopMonthComic, newItem: TopMonthComic) = oldItem == newItem
}