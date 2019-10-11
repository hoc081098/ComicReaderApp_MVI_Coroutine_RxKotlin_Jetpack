package com.hoc.comicapp.ui.category_detail

import android.annotation.SuppressLint
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.*
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.HeaderType.Popular
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.HeaderType.Updated
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.Item
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.recyclerview.scrollStateChanges
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.item_recycler_category_detail_comic.view.*
import kotlinx.android.synthetic.main.item_recycler_category_detail_error.view.*
import kotlinx.android.synthetic.main.item_recycler_category_detail_header.view.*
import kotlinx.android.synthetic.main.item_recycler_category_detail_popular_horizontal_recycler.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

private object ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
  override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
    return when {
      oldItem is Item.PopularVS && newItem is Item.PopularVS -> true
      oldItem is Item.Comic && newItem is Item.Comic -> oldItem.link == newItem.link
      oldItem == Item.Loading && newItem is Item.Loading -> true
      oldItem is Item.Error && newItem is Item.Error -> true
      else -> oldItem == newItem
    }
  }

  @SuppressLint("DiffUtilEquals")
  override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean = oldItem == newItem
}

class CategoryDetailAdapter(
  private val glide: GlideRequests,
  private val lifecycleOwner: LifecycleOwner
  ) :
  ListAdapter<Item, CategoryDetailAdapter.VH>(ItemDiffCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, @LayoutRes viewType: Int): VH {
    val itemView = parent inflate viewType
    return when (viewType) {
      R.layout.item_recycler_category_detail_popular_horizontal_recycler -> PopularHorizontalRecyclerVH(itemView)
      R.layout.item_recycler_category_detail_comic -> ComicVH(itemView)
      R.layout.item_recycler_category_detail_loading -> LoadingVH(itemView)
      R.layout.item_recycler_category_detail_error -> ErrorVH(itemView)
      R.layout.item_recycler_category_detail_header -> HeaderVH(itemView)
      else -> error("Don't know viewType=$viewType")
    }
  }

  @LayoutRes
  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is Item.PopularVS -> R.layout.item_recycler_category_detail_popular_horizontal_recycler
      is Item.Comic -> R.layout.item_recycler_category_detail_comic
      Item.Loading -> R.layout.item_recycler_category_detail_loading
      is Item.Error -> R.layout.item_recycler_category_detail_error
      is Item.Header -> R.layout.item_recycler_category_detail_header
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: Item)
  }

  private inner class PopularHorizontalRecyclerVH(itemView: View) : VH(itemView) {
    private val recycler = itemView.popular_recycler_horizontal!!
    private val progressBar = itemView.popular_progress_bar!!
    private val textError = itemView.popular_error_message!!
    private val buttonRetry = itemView.button_popular_horizontal_retry!!
    private val errorGroup = itemView.error_group!!

    private val adapter = PopularHorizontalAdapter(glide)
    private var latestComics: List<CategoryDetailContract.ViewState.PopularItem>? = null

    private val startStopAutoScrollS = PublishRelay.create<Boolean>()
    private val intervalInMillis = 1_200L
    private val delayAfterTouchInMillis = 3_000L

    init {
      recycler.run {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = this@PopularHorizontalRecyclerVH.adapter
        LinearSnapHelper().attachToRecyclerView(this)
      }

      val smoothScroller = object : LinearSmoothScroller(itemView.context) {
        override fun getVerticalSnapPreference(): Int {
          return SNAP_TO_ANY
        }

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
          return 120f / displayMetrics.densityDpi
        }
      }

      lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        var disposable: Disposable?=null

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        private fun onCreate() {
          disposable = startStopAutoScrollS
            .mergeWith(
              recycler
                .scrollStateChanges()
                .filter { it == RecyclerView.SCROLL_STATE_DRAGGING }
                .switchMap {
                  Observable.just(false)
                    .concatWith(
                      Observable.timer(
                        delayAfterTouchInMillis,
                        TimeUnit.MILLISECONDS
                      ).map { true }
                    )
                }
            )
            .map { it to adapter.itemCount }
            .switchMap { (startAutoScroll, itemCount) ->
              if (!startAutoScroll || itemCount == 0) {
                Observable.just(-1L)
              } else {
                Observable
                  .interval(intervalInMillis, TimeUnit.MILLISECONDS)
                  .map { it % itemCount }
              }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
              onNext = {
                if (it >= 0 && it != 2L) {
                  recycler
                    .layoutManager
                    ?.startSmoothScroll(smoothScroller.apply { targetPosition = it.toInt() })
                }
              },
              onError = {}
            )
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        private fun onResume() = startStopAutoScrollS.accept(true)

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        private fun onPause() = startStopAutoScrollS.accept(false)

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        private fun onDestroy() = disposable?.dispose()
      })
    }

    override fun bind(item: Item) = onlyBind<Item.PopularVS>(item) { (comics, error, isLoading) ->
      errorGroup.isVisible = error !== null
      textError.text = error?.getMessage()

      progressBar.isVisible = isLoading

      if (latestComics != comics) {
        adapter.submitList(comics)
        latestComics = comics
        startStopAutoScrollS.accept(true)
        Timber.d("comics.size=${comics.size}")
      } else {
        Timber.d("comics.size=${comics.size} == ")
      }
    }
  }

  private inner class ComicVH(itemView: View) : VH(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textView = itemView.text_view!!

    private val textChapterName3 = itemView.text_chapter_name_3!!
    private val textChapterName2 = itemView.text_chapter_name_2!!
    private val textChapterName1 = itemView.text_chapter_name_1!!

    private val textChapterTime3 = itemView.text_chapter_time_3!!
    private val textChapterTime2 = itemView.text_chapter_time_2!!
    private val textChapterTime1 = itemView.text_chapter_time_1!!

    private val textChapters = arrayOf(
      textChapterName3 to textChapterTime3,
      textChapterName2 to textChapterTime2,
      textChapterName1 to textChapterTime1
    )

    override fun bind(item: Item) = onlyBind<Item.Comic>(item) { comic ->
      glide
        .load(comic.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageComic)

      textComicName.text = comic.title
      textView.text = comic.view

      textChapters
        .zip(comic.lastChapters)
        .forEach { (textViews, chapter) ->
          textViews.first.text = chapter.chapterName
          textViews.second.text = chapter.time
        }
    }
  }

  private class LoadingVH(itemView: View) : VH(itemView) {
    override fun bind(item: Item) = onlyBind<Item.Loading>(item) {}
  }

  private inner class ErrorVH(itemView: View) : VH(itemView) {
    private val textErrorMessage = itemView.text_error_message!!
    private val buttonRetry = itemView.button_retry!!

    override fun bind(item: Item) = onlyBind<Item.Error>(item) { (error) ->
      textErrorMessage.text = error.getMessage()
    }
  }

  private inner class HeaderVH(itemView: View) : VH(itemView) {
    private val textHeader = itemView.text_header!!

    override fun bind(item: Item) = onlyBind<Item.Header>(item) { (type) ->
      textHeader.text = when (type) {
        Popular -> "Popular"
        Updated -> "Latest updated"
      }
    }
  }

  private companion object {
    /**
     * @throws IllegalStateException
     */
    inline fun <reified T : Item> VH.onlyBind(
      item: Item,
      crossinline bind: (T) -> Unit
    ) {
      check(item is T) { "${this::class.java.simpleName}::bind only accept ${T::class.java.simpleName}, but item=$item" }
      bind(item)
    }
  }
}
