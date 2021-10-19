package com.hoc.comicapp.ui.category_detail

import android.annotation.SuppressLint
import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.ItemRecyclerCategoryDetailComicBinding
import com.hoc.comicapp.databinding.ItemRecyclerCategoryDetailErrorBinding
import com.hoc.comicapp.databinding.ItemRecyclerCategoryDetailHeaderBinding
import com.hoc.comicapp.databinding.ItemRecyclerCategoryDetailLoadingBinding
import com.hoc.comicapp.databinding.ItemRecyclerCategoryDetailPopularHorizontalRecyclerBinding
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.HeaderType.Popular
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.HeaderType.Updated
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.Item
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflater
import com.hoc.comicapp.utils.unit
import com.jakewharton.rxbinding4.recyclerview.scrollStateChanges
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.detaches
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
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
  private val lifecycleOwner: LifecycleOwner,
  private val compositeDisposable: CompositeDisposable,
  private val onClickComic: (Arguments.ComicDetailArgs) -> Unit,
) :
  ListAdapter<Item, CategoryDetailAdapter.VH>(ItemDiffCallback) {
  private var outLayoutManagerSavedState: Parcelable? = null

  private val _retryPopularS = PublishRelay.create<Unit>()
  val retryPopularObservable get() = _retryPopularS.asObservable()

  private val _retryS = PublishRelay.create<Unit>()
  val retryObservable get() = _retryS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, @LayoutRes viewType: Int): VH {
    return when (viewType) {
      R.layout.item_recycler_category_detail_popular_horizontal_recycler -> PopularHorizontalRecyclerVH(
        ItemRecyclerCategoryDetailPopularHorizontalRecyclerBinding.inflate(
          parent.inflater,
          parent,
          false
        ),
        parent
      )
      R.layout.item_recycler_category_detail_comic -> ComicVH(
        ItemRecyclerCategoryDetailComicBinding.inflate(
          parent.inflater,
          parent,
          false
        )
      )
      R.layout.item_recycler_category_detail_loading -> LoadingVH(
        ItemRecyclerCategoryDetailLoadingBinding.inflate(
          parent.inflater,
          parent,
          false
        )
      )
      R.layout.item_recycler_category_detail_error -> ErrorVH(
        ItemRecyclerCategoryDetailErrorBinding.inflate(
          parent.inflater,
          parent,
          false
        ),
        parent
      )
      R.layout.item_recycler_category_detail_header -> HeaderVH(
        ItemRecyclerCategoryDetailHeaderBinding.inflate(
          parent.inflater,
          parent,
          false
        ),
      )
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

  override fun onViewRecycled(holder: VH) {
    super.onViewRecycled(holder)
    if (holder is PopularHorizontalRecyclerVH) {
      outLayoutManagerSavedState = holder.linearLayoutManager.onSaveInstanceState()
      Timber.tag("@@@").d("onViewRecycled $outLayoutManagerSavedState")
    }
  }

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: Item)
  }

  private inner class PopularHorizontalRecyclerVH(
    private val binding: ItemRecyclerCategoryDetailPopularHorizontalRecyclerBinding,
    parent: ViewGroup,
  ) :
    VH(binding.root) {
    private val adapter = PopularHorizontalAdapter(glide, onClickComic)
    private var latestComics: List<CategoryDetailContract.ViewState.PopularItem>? = null

    private val startStopAutoScrollS = PublishRelay.create<Boolean>()
    private val intervalInMillis = 1_200L
    private val delayAfterTouchInMillis = 3_000L

    val linearLayoutManager: LinearLayoutManager

    init {
      binding.buttonPopularHorizontalRetry
        .clicks()
        .takeUntil(parent.detaches())
        .subscribe(_retryPopularS)
        .addTo(compositeDisposable)

      binding.popularRecyclerHorizontal.run {
        setHasFixedSize(true)
        linearLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        layoutManager = linearLayoutManager
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

      lifecycleOwner
        .lifecycle
        .addObserver(
          object : DefaultLifecycleObserver {
            var disposable: Disposable? = null

            override fun onCreate(owner: LifecycleOwner) {
              disposable = startStopAutoScrollS
                .mergeWith(
                  binding.popularRecyclerHorizontal
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
                      binding.popularRecyclerHorizontal
                        .layoutManager
                        ?.startSmoothScroll(smoothScroller.apply { targetPosition = it.toInt() })
                    }
                  },
                  onError = {}
                )
            }

            override fun onResume(owner: LifecycleOwner) = startStopAutoScrollS.accept(true)

            override fun onPause(owner: LifecycleOwner) = startStopAutoScrollS.accept(false)

            override fun onDestroy(owner: LifecycleOwner) = disposable?.dispose().unit
          }
        )
    }

    override fun bind(item: Item) =
      onlyBind<Item.PopularVS, ItemRecyclerCategoryDetailPopularHorizontalRecyclerBinding>(
        item,
        binding
      ) { (comics, error, isLoading) ->
        errorGroup.isVisible = error !== null
        popularErrorMessage.text = error?.getMessage()

        popularProgressBar.isVisible = isLoading

        if (latestComics != comics) {
          adapter.submitList(comics) {
            if (comics.isNotEmpty()) {
              popularRecyclerHorizontal.scrollToPosition(0)
            }
            startStopAutoScrollS.accept(comics.size > 1)
          }
          latestComics = comics
          Timber.d("comics.size=${comics.size}")
        } else {
          Timber.d("comics.size=${comics.size} == ")
        }

        outLayoutManagerSavedState?.let {
          Timber.tag("@@@").d("onRestoreInstanceState $it")
          linearLayoutManager.onRestoreInstanceState(it)
          outLayoutManagerSavedState = null
        }
      }
  }

  private inner class ComicVH(private val binding: ItemRecyclerCategoryDetailComicBinding) :
    VH(binding.root) {
    private val textChapters = binding.run {
      arrayOf(
        textChapterName3 to textChapterTime3,
        textChapterName2 to textChapterTime2,
        textChapterName1 to textChapterTime1
      )
    }

    init {
      itemView.setOnClickListener {
        val position = bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
          val item = getItem(position) as? Item.Comic ?: return@setOnClickListener
          onClickComic(
            Arguments.ComicDetailArgs(
              title = item.title,
              link = item.link,
              thumbnail = item.thumbnail,
              view = item.view,
              remoteThumbnail = item.thumbnail
            )
          )
        }
      }
    }

    override fun bind(item: Item) =
      onlyBind<Item.Comic, ItemRecyclerCategoryDetailComicBinding>(item, binding) { comic ->
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

  private class LoadingVH(private val binding: ItemRecyclerCategoryDetailLoadingBinding) :
    VH(binding.root) {
    override fun bind(item: Item) =
      onlyBind<Item.Loading, ItemRecyclerCategoryDetailLoadingBinding>(item, binding) {}
  }

  private inner class ErrorVH(
    private val binding: ItemRecyclerCategoryDetailErrorBinding,
    parent: ViewGroup,
  ) : VH(binding.root) {
    init {
      binding.buttonRetry
        .clicks()
        .takeUntil(parent.detaches())
        .subscribe(_retryS)
        .addTo(compositeDisposable)
    }

    override fun bind(item: Item) =
      onlyBind<Item.Error, ItemRecyclerCategoryDetailErrorBinding>(item, binding) { (error) ->
        textErrorMessage.text = error.getMessage()
      }
  }

  private inner class HeaderVH(private val binding: ItemRecyclerCategoryDetailHeaderBinding) :
    VH(binding.root) {
    override fun bind(item: Item) =
      onlyBind<Item.Header, ItemRecyclerCategoryDetailHeaderBinding>(item, binding) { (type) ->
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
    inline fun <reified T : Item, B : ViewBinding> VH.onlyBind(
      item: Item,
      binding: B,
      crossinline bind: B.(T) -> Unit,
    ) {
      check(item is T) { "${this::class.java.simpleName}::bind only accept ${T::class.java.simpleName}, but item=$item" }
      binding.bind(item)
    }
  }
}
