package com.hoc.comicapp.ui.home

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.ItemRecyclerviewTopMonthComicOrRecommenedBinding
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.ui.home.HomeAdapter.Companion.MOST_VIEW_COMIC_ITEM_VIEW_TYPE
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflater
import com.hoc.comicapp.utils.mapNotNull
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

class MostViewedAdapter(
  private val glide: GlideRequests,
  private val compositeDisposable: CompositeDisposable,
) :
  ListAdapter<Comic, MostViewedAdapter.VH>(MostViewedComicDiffUtilItemCallback) {
  private val clickComicS = PublishRelay.create<_HomeClickEvent>()
  val clickComicObservable get() = clickComicS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    when (viewType) {
      MOST_VIEW_COMIC_ITEM_VIEW_TYPE -> return VH(
        ItemRecyclerviewTopMonthComicOrRecommenedBinding.inflate(
          parent.inflater,
          parent,
          false,
        )
      )
      else -> throw IllegalStateException("viewType must be $MOST_VIEW_COMIC_ITEM_VIEW_TYPE, but viewType=$viewType")
    }
  }

  override fun getItemViewType(position: Int) = MOST_VIEW_COMIC_ITEM_VIEW_TYPE

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(private val binding: ItemRecyclerviewTopMonthComicOrRecommenedBinding) :
    RecyclerView.ViewHolder(binding.root) {
    init {
      itemView
        .clicks()
        .mapNotNull {
          when (val position = bindingAdapterPosition) {
            RecyclerView.NO_POSITION -> null
            else -> getItem(position).let {
              Triple(
                itemView,
                it,
                "most_viewed#${it.link}",
              )
            }
          }
        }
        .subscribe(clickComicS)
        .addTo(compositeDisposable)
    }

    fun bind(item: Comic) = binding.run {
      itemView.transitionName = "most_viewed#${item.link}"

      textComicName.text = item.title
      textChapter.text = item.lastChapters.lastOrNull()?.chapterName
      textViewOrLastUpdatedTime.text = item.view

      glide
        .load(item.thumbnail)
        .placeholder(R.drawable.splash_background)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)

      Unit
    }
  }
}
