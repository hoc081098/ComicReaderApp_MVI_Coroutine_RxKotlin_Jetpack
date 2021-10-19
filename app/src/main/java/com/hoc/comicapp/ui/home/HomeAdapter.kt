package com.hoc.comicapp.ui.home

import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.ItemRecyclerHomeHeaderBinding
import com.hoc.comicapp.databinding.ItemRecyclerHomeRecyclerBinding
import com.hoc.comicapp.databinding.ItemRecyclerviewUpdatedComicBinding
import com.hoc.comicapp.databinding.ItemRecyclerviewUpdatedErrorBinding
import com.hoc.comicapp.databinding.ItemRecyclerviewUpdatedLoadingBinding
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.MOST_VIEWED
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.NEWEST
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.UPDATED
import com.hoc.comicapp.utils.mapNotNull
import com.hoc.comicapp.utils.unit
import com.hoc081098.viewbindingdelegate.inflateViewBinding
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

typealias HomeClickEvent = Triple<View, Arguments.ComicDetailArgs, String>
typealias _HomeClickEvent = Triple<View, Comic, String>

private fun toHomeClickEvent(event: _HomeClickEvent): HomeClickEvent {
  return Triple(
    event.first,
    event.second.let { comic ->
      Arguments.ComicDetailArgs(
        link = comic.link,
        thumbnail = comic.thumbnail,
        title = comic.title,
        view = comic.view,
        remoteThumbnail = comic.thumbnail
      )
    },
    event.third
  )
}

class HomeAdapter(
  var lifecycleOwner: LifecycleOwner,
  private val glide: GlideRequests,
  private val viewPool: RecyclerView.RecycledViewPool,
  private val compositeDisposable: CompositeDisposable,
) :
  ListAdapter<HomeListItem, HomeAdapter.VH>(HomeListItemDiffUtilItemCallback) {
  // Layout manager saved states
  private var newestLayoutManagerSavedState: Parcelable? = null
  private var mostViewedLayoutManagerSavedState: Parcelable? = null

  // Adapters
  private val newestAdapter =
    NewestAdapter(glide, compositeDisposable).apply { submitList(emptyList()) }
  private val mostViewedAdapter =
    MostViewedAdapter(glide, compositeDisposable).apply { submitList(emptyList()) }

  // Retry relays
  private val newestRetryS = PublishRelay.create<Unit>()
  private val mostViewedRetryS = PublishRelay.create<Unit>()
  private val updatedRetryS = PublishRelay.create<Unit>()

  // Click relay
  private val clickComicS = PublishRelay.create<_HomeClickEvent>()

  // Retry observables
  val newestRetryObservable: Observable<Unit> = newestRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)
  val mostViewedRetryObservable: Observable<Unit> = mostViewedRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)
  val updatedRetryObservable: Observable<Unit> = updatedRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)

  // Click observables
  val clickComicObservable: Observable<HomeClickEvent> = Observable.mergeArray(
    newestAdapter.clickComicObservable,
    mostViewedAdapter.clickComicObservable,
    clickComicS,
  )
    .map(::toHomeClickEvent)
    .doOnNext { Timber.d("[*] Click comic $it") }

  override fun onCreateViewHolder(parent: ViewGroup, @ViewType viewType: Int): VH {
    return when (viewType) {
      NEWEST_LIST_VIEW_TYPE -> NewestComicsListVH(parent inflateViewBinding false)
      MOST_VIEWED_LIST_VIEW_TYPE -> MostViewedComicsListVH(parent inflateViewBinding false)
      COMIC_ITEM_VIEW_TYPE -> ComicItemVH(parent inflateViewBinding false, parent)
      ERROR_ITEM_VIEW_TYPE -> ErrorVH(parent inflateViewBinding false)
      LOADING_ITEM_VIEW_TYPE -> LoadingVH(parent inflateViewBinding false)
      HEADER_VIEW_TYPE -> HeaderVH(parent inflateViewBinding false)
      else -> error("Unknown view type: $viewType")
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  @ViewType
  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is HomeListItem.NewestListState -> NEWEST_LIST_VIEW_TYPE
      is HomeListItem.MostViewedListState -> MOST_VIEWED_LIST_VIEW_TYPE
      is HomeListItem.UpdatedItem.ComicItem -> COMIC_ITEM_VIEW_TYPE
      is HomeListItem.UpdatedItem.Error -> ERROR_ITEM_VIEW_TYPE
      HomeListItem.UpdatedItem.Loading -> LOADING_ITEM_VIEW_TYPE
      is HomeListItem.Header -> HEADER_VIEW_TYPE
    }
  }

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: HomeListItem)
  }

  private abstract inner class HorizontalRecyclerVH(protected val binding: ItemRecyclerHomeRecyclerBinding) :
    VH(binding.root) {
    protected val recycler = binding.homeRecyclerHorizontal.apply { setRecycledViewPool(viewPool) }
    protected val progressBar get() = binding.homeProgressBar
    protected val textErrorMessage get() = binding.homeErrorMessage
    protected val buttonRetry get() = binding.buttonHomeHorizontalRetry
  }

  private inner class NewestComicsListVH(binding: ItemRecyclerHomeRecyclerBinding) :
    HorizontalRecyclerVH(binding) {
    private var currentList = emptyList<Comic>()
    val linearLayoutManager: LinearLayoutManager

    private val startStopAutoScrollS = PublishRelay.create<Boolean>()
    private val intervalInMillis = 1_500L
    private val delayAfterTouchInMillis = 3_000L

    init {
      Timber.d("[###] SuggestListVH::init")
      buttonRetry.setOnClickListener { newestRetryS.accept(Unit) }

      recycler.run {
        setHasFixedSize(true)
        linearLayoutManager = LinearLayoutManager(
          context,
          RecyclerView.HORIZONTAL,
          false,
        ).also { layoutManager = it }
        adapter = newestAdapter
        LinearSnapHelper().attachToRecyclerView(this)
      }

      observeLifecycle()
    }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.NewestListState, ItemRecyclerHomeRecyclerBinding>(
        item,
        binding
      ) { (comics, errorMessage, isLoading) ->
        Timber.d("suggest_state=[$isLoading, $errorMessage, $comics]")

        errorGroup.isVisible = errorMessage !== null
        textErrorMessage.text = errorMessage

        progressBar.isVisible = isLoading

        if (currentList != comics) {
          newestAdapter.submitList(comics) {
            startStopAutoScrollS.accept(comics.size > 1)
          }
          currentList = comics
        }

        newestLayoutManagerSavedState?.let {
          Timber.tag("[preserve]").d("[bind] [1] $it")
          linearLayoutManager.onRestoreInstanceState(it)
          newestLayoutManagerSavedState = null
        }
      }

    private fun observeLifecycle() {
      Timber.d("[>>>] NewestComicsListVH::init with ${lifecycleOwner.lifecycle}")

      val smoothScroller = object : LinearSmoothScroller(itemView.context) {
        override fun getVerticalSnapPreference(): Int {
          return SNAP_TO_ANY
        }

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
          return 120f / displayMetrics.densityDpi
        }
      }

      lifecycleOwner.lifecycle.addObserver(
        object : DefaultLifecycleObserver {
          var disposable: Disposable? = null

          override fun onCreate(owner: LifecycleOwner) {
            disposable = startStopAutoScrollS
              .doOnNext { Timber.d("[###] [1] $it") }
              .concatMap {
                if (it) {
                  Observable.timer(delayAfterTouchInMillis, TimeUnit.MILLISECONDS).map { true }
                } else {
                  Observable.just(false)
                }
              }
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
                  .doOnNext { Timber.d("[###] [2] $it") }
              )
              .map { it to newestAdapter.itemCount }
              .distinctUntilChanged()
              .doOnNext { Timber.d("[###] [3] $it") }
              .switchMap { (startAutoScroll, itemCount) ->
                if (!startAutoScroll || itemCount == 0) {
                  Observable.just(-1L)
                } else {
                  Observable
                    .interval(0, intervalInMillis, TimeUnit.MILLISECONDS)
                    .map { it % itemCount }
                }
              }
              .doOnNext { Timber.d("[###] [4] $it") }
              .observeOn(AndroidSchedulers.mainThread())
              .subscribeBy(
                onNext = {
                  if (it >= 0) {
                    recycler
                      .layoutManager
                      ?.startSmoothScroll(smoothScroller.apply { targetPosition = it.toInt() })
                  }
                },
                onError = {}
              )
            Timber.d("[>>>] ON_CREATE")
          }

          override fun onResume(owner: LifecycleOwner) = startStopAutoScrollS.accept(true)
            .also { Timber.d("[>>>] ON_RESUME -> start") }

          override fun onPause(owner: LifecycleOwner) = startStopAutoScrollS.accept(false)
            .also { Timber.d("[>>>] ON_PAUSE -> stop") }

          override fun onDestroy(owner: LifecycleOwner) = disposable?.dispose()
            .also { lifecycleOwner.lifecycle.removeObserver(this) }
            .also { Timber.d("[>>>] ON_DESTROY -> disposed") }
            .unit
        }
      )
    }
  }

  private inner class MostViewedComicsListVH(binding: ItemRecyclerHomeRecyclerBinding) :
    HorizontalRecyclerVH(binding) {
    private var currentList = emptyList<Comic>()

    val linearLayoutManager: LinearLayoutManager

    init {
      Timber.d("[###] TopMonthListVH::init")
      buttonRetry.setOnClickListener { mostViewedRetryS.accept(Unit) }

      recycler.run {
        setHasFixedSize(true)
        linearLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        layoutManager = linearLayoutManager
        adapter = mostViewedAdapter
      }
    }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.MostViewedListState, ItemRecyclerHomeRecyclerBinding>(
        item,
        binding
      ) { (comics, errorMessage, isLoading) ->
        Timber.d("top_month_state=[$isLoading, $errorMessage, $comics]")

        errorGroup.isVisible = errorMessage !== null
        textErrorMessage.text = errorMessage

        progressBar.isVisible = isLoading

        if (currentList != comics) {
          mostViewedAdapter.submitList(comics)
          currentList = comics
        }

        mostViewedLayoutManagerSavedState?.let {
          Timber.tag("[preserve]").d("[bind] [2] $it")
          linearLayoutManager.onRestoreInstanceState(it)
          mostViewedLayoutManagerSavedState = null
        }
      }
  }

  private inner class ComicItemVH(
    private val binding: ItemRecyclerviewUpdatedComicBinding,
    parent: View,
  ) : VH(binding.root) {

    private val textChapters = binding.run {
      listOf(
        textChapterName3 to textChapterTime3,
        textChapterName2 to textChapterTime2,
        textChapterName1 to textChapterTime1
      )
    }

    init {
      itemView
        .clicks()
        .takeUntil(parent.detaches())
        .mapNotNull {
          when (val position = bindingAdapterPosition) {
            RecyclerView.NO_POSITION -> null
            else -> when (val item = getItem(position)) {
              is HomeListItem.UpdatedItem.ComicItem -> Triple(
                itemView,
                item.comic,
                "updated#${item.comic.link}",
              )
              else -> null
            }
          }
        }
        .subscribe(clickComicS)
        .addTo(compositeDisposable)
    }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.UpdatedItem.ComicItem, ItemRecyclerviewUpdatedComicBinding>(
        item,
        binding
      ) { (comic) ->
        itemView.transitionName = "updated#${comic.link}"

        glide
          .load(comic.thumbnail)
          .placeholder(R.drawable.splash_background)
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

  private class LoadingVH(private val binding: ItemRecyclerviewUpdatedLoadingBinding) :
    VH(binding.root) {
    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.UpdatedItem.Loading, ItemRecyclerviewUpdatedLoadingBinding>(
        item,
        binding
      )
  }

  private inner class ErrorVH(private val binding: ItemRecyclerviewUpdatedErrorBinding) :
    VH(binding.root) {
    init {
      binding.buttonUpdatedRetry.setOnClickListener { updatedRetryS.accept(Unit) }
    }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.UpdatedItem.Error, ItemRecyclerviewUpdatedErrorBinding>(
        item,
        binding
      ) { (errorMessage) ->
        textUpdatedErrorMessage.text = errorMessage
      }
  }

  private class HeaderVH(private val binding: ItemRecyclerHomeHeaderBinding) : VH(binding.root) {
    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.Header, ItemRecyclerHomeHeaderBinding>(item, binding) { (type) ->
        textHomeHeader.text = when (type) {
          NEWEST -> "Newest"
          MOST_VIEWED -> "Most viewed"
          UPDATED -> "Updated"
        }
      }
  }

  override fun onViewRecycled(holder: VH) {
    super.onViewRecycled(holder)

    when (holder) {
      is NewestComicsListVH -> {
        newestLayoutManagerSavedState = holder.linearLayoutManager.onSaveInstanceState().also {
          Timber.tag("[preserve]").d("[onViewRecycled] [1] $it")
        }
      }
      is MostViewedComicsListVH -> {
        mostViewedLayoutManagerSavedState = holder.linearLayoutManager.onSaveInstanceState().also {
          Timber.tag("[preserve]").d("[onViewRecycled] [2] $it")
        }
      }
    }
  }

  companion object {
    const val NEWEST_LIST_VIEW_TYPE = 1
    const val MOST_VIEWED_LIST_VIEW_TYPE = 2
    const val COMIC_ITEM_VIEW_TYPE = 3
    const val ERROR_ITEM_VIEW_TYPE = 4
    const val LOADING_ITEM_VIEW_TYPE = 5
    const val HEADER_VIEW_TYPE = 6
    const val NEWEST_COMIC_ITEM_VIEW_TYPE = 7
    const val MOST_VIEW_COMIC_ITEM_VIEW_TYPE = 8

    @IntDef(
      value = [
        NEWEST_LIST_VIEW_TYPE,
        MOST_VIEWED_LIST_VIEW_TYPE,
        COMIC_ITEM_VIEW_TYPE,
        ERROR_ITEM_VIEW_TYPE,
        LOADING_ITEM_VIEW_TYPE,
        HEADER_VIEW_TYPE
      ]
    )
    @Retention(AnnotationRetention.SOURCE)
    private annotation class ViewType

    /**
     * @throws IllegalStateException
     */
    private inline fun <reified T : HomeListItem, B : ViewBinding> VH.onlyBind(
      item: HomeListItem,
      binding: B,
      crossinline bind: B.(T) -> Unit = {},
    ) {
      check(item is T) { "${this::class.java.simpleName}::bind only accept ${T::class.java.simpleName}, but item=$item" }
      binding.bind(item)
    }
  }
}
