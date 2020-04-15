package com.hoc.comicapp.ui.home

import android.os.Parcelable
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.Comic
import com.hoc.comicapp.ui.detail.ComicArg
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.MOST_VIEWED
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.NEWEST
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.UPDATED
import com.hoc.comicapp.utils.inflate
import com.hoc.comicapp.utils.mapNotNull
import com.jakewharton.rxbinding3.recyclerview.scrollStateChanges
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.item_recycler_home_header.view.*
import kotlinx.android.synthetic.main.item_recycler_home_recycler.view.*
import kotlinx.android.synthetic.main.item_recyclerview_updated_comic.view.*
import kotlinx.android.synthetic.main.item_recyclerview_updated_error.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

typealias HomeClickEvent = Triple<View, ComicArg, String>
typealias _HomeClickEvent = Triple<View, Comic, String>

private fun toHomeClickEvent(event: _HomeClickEvent): HomeClickEvent {
  return Triple(
    event.first,
    event.second.let { comic ->
      ComicArg(
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
  val newestRetryObservable = newestRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)!!
  val mostViewedRetryObservable = mostViewedRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)!!
  val updatedRetryObservable = updatedRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)!!

  // Click observables
  val clickComicObservable: Observable<HomeClickEvent> = Observable.mergeArray(
      newestAdapter.clickComicObservable,
      mostViewedAdapter.clickComicObservable,
      clickComicS,
    )
    .map(::toHomeClickEvent)
    .doOnNext { Timber.d("[*] Click comic $it") }!!

  override fun onCreateViewHolder(parent: ViewGroup, @ViewType viewType: Int): VH {
    return when (viewType) {
      NEWEST_LIST_VIEW_TYPE -> NewestComicsListVH(parent inflate R.layout.item_recycler_home_recycler)
      MOST_VIEWED_LIST_VIEW_TYPE -> MostViewedComicsListVH(parent inflate R.layout.item_recycler_home_recycler)
      COMIC_ITEM_VIEW_TYPE -> ComicItemVH(
        parent inflate R.layout.item_recyclerview_updated_comic,
        parent,
      )
      ERROR_ITEM_VIEW_TYPE -> ErrorVH(parent inflate R.layout.item_recyclerview_updated_error)
      LOADING_ITEM_VIEW_TYPE -> LoadingVH(parent inflate R.layout.item_recyclerview_updated_loading)
      HEADER_VIEW_TYPE -> HeaderVH(parent inflate R.layout.item_recycler_home_header)
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

  private abstract inner class HorizontalRecyclerVH(itemView: View) : VH(itemView) {
    protected val recycler =
      itemView.home_recycler_horizontal!!.apply { setRecycledViewPool(viewPool) }
    protected val progressBar = itemView.home_progress_bar!!
    protected val textErrorMessage = itemView.home_error_message!!
    protected val buttonRetry = itemView.button_home_horizontal_retry!!
    protected val errorGroup = itemView.error_group!!
  }

  private inner class NewestComicsListVH(itemView: View) : HorizontalRecyclerVH(itemView) {
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
      onlyBind<HomeListItem.NewestListState>(item) { (comics, errorMessage, isLoading) ->
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

      lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        var disposable: Disposable? = null

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        private fun onCreate() {
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

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        private fun onResume() = startStopAutoScrollS.accept(true)
          .also { Timber.d("[>>>] ON_RESUME -> start") }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        private fun onPause() = startStopAutoScrollS.accept(false)
          .also { Timber.d("[>>>] ON_PAUSE -> stop") }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        private fun onDestroy() = disposable?.dispose()
          .also { lifecycleOwner.lifecycle.removeObserver(this) }
          .also { Timber.d("[>>>] ON_DESTROY -> disposed") }
      })
    }
  }

  private inner class MostViewedComicsListVH(itemView: View) : HorizontalRecyclerVH(itemView) {
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
      onlyBind<HomeListItem.MostViewedListState>(item) { (comics, errorMessage, isLoading) ->
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

  private inner class ComicItemVH(itemView: View, parent: View) : VH(itemView) {
    val imageComic = itemView.image_comic!!
    private val textView = itemView.text_view!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapterName3 = itemView.text_chapter_name_3!!
    private val textChapterTime3 = itemView.text_chapter_time_3!!
    private val textChapterName2 = itemView.text_chapter_name_2!!
    private val textChapterTime2 = itemView.text_chapter_time_2!!
    private val textChapterName1 = itemView.text_chapter_name_1!!
    private val textChapterTime1 = itemView.text_chapter_time_1!!

    private val textChapters = listOf(
      textChapterName3 to textChapterTime3,
      textChapterName2 to textChapterTime2,
      textChapterName1 to textChapterTime1
    )

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
      onlyBind<HomeListItem.UpdatedItem.ComicItem>(item) { (comic) ->
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

  private class LoadingVH(itemView: View) : VH(itemView) {
    override fun bind(item: HomeListItem) = onlyBind<HomeListItem.UpdatedItem.Loading>(item) {}
  }

  private inner class ErrorVH(itemView: View) : VH(itemView) {
    private val textErrorMessage = itemView.text_updated_error_message!!
    private val buttonRetry = itemView.button_updated_retry!!

    init {
      buttonRetry.setOnClickListener { updatedRetryS.accept(Unit) }
    }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.UpdatedItem.Error>(item) { (errorMessage) ->
        textErrorMessage.text = errorMessage
      }
  }

  private class HeaderVH(itemView: View) : VH(itemView) {
    private val textHeader = itemView.text_home_header!!

    override fun bind(item: HomeListItem) = onlyBind<HomeListItem.Header>(item) { (type) ->
      textHeader.text = when (type) {
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
    private inline fun <reified T : HomeListItem> VH.onlyBind(
      item: HomeListItem,
      crossinline bind: (T) -> Unit,
    ) {
      check(item is T) { "${this::class.java.simpleName}::bind only accept ${T::class.java.simpleName}, but item=$item" }
      bind(item)
    }
  }
}


