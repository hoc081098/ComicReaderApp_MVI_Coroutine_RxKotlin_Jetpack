package com.hoc.comicapp.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.ui.home.HomeAdapter.Companion.NEWEST_COMIC_ITEM_VIEW_TYPE
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.hoc.comicapp.utils.mapNotNull
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import kotlinx.android.synthetic.main.item_recyclerview_top_month_comic_or_recommened.view.*

class NewestAdapter(
  private val glide: GlideRequests,
  private val compositeDisposable: CompositeDisposable,
) : ListAdapter<Comic, NewestAdapter.VH>(NewestComicDiffUtilItemCallback) {
  private val clickComicS = PublishRelay.create<_HomeClickEvent>()
  val clickComicObservable get() = clickComicS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return when (viewType) {
      NEWEST_COMIC_ITEM_VIEW_TYPE -> VH(parent inflate R.layout.item_recyclerview_top_month_comic_or_recommened)
      else -> throw IllegalStateException("viewType must be $NEWEST_COMIC_ITEM_VIEW_TYPE, but viewType=$viewType")
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) =
    holder.bind(getItem(position % itemCount))

  override fun getItemViewType(position: Int) = NEWEST_COMIC_ITEM_VIEW_TYPE

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!
    private val textLastUpdatedTime = itemView.text_view_or_last_updated_time!!
    private val imageIconClock = itemView.image_eye_or_clock!!

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
                "newest#${it.link}",
              )
            }
          }
        }
        .subscribe(clickComicS)
        .addTo(compositeDisposable)
    }

    fun bind(item: Comic) {
      itemView.transitionName = "newest#${item.link}"

      textComicName.text = item.title
      textChapter.text = item.lastChapters.lastOrNull()?.chapterName
      textLastUpdatedTime.text = item.lastChapters.lastOrNull()?.time
      imageIconClock.setImageResource(R.drawable.ic_access_time_white_24dp)

      glide
        .load(item.thumbnail)
        .placeholder(R.drawable.splash_background)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)
    }
  }
}