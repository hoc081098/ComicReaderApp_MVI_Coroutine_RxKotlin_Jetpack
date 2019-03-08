package com.hoc.comicapp.ui.home

import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.SUGGEST
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.TOP_MONTH
import com.hoc.comicapp.ui.home.HomeListItem.HeaderType.UPDATED
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.recyclerview.scrollStateChanges
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.ofType
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.item_recycler_home_header.view.*
import kotlinx.android.synthetic.main.item_recycler_home_recycler.view.*
import kotlinx.android.synthetic.main.item_recyclerview_updated_comic.view.*
import kotlinx.android.synthetic.main.item_recyclerview_updated_error.view.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class HomeAdapter(private val lifecycleOwner: LifecycleOwner) :
  ListAdapter<HomeListItem, HomeAdapter.VH>(HomeListItemDiffUtilItemCallback) {
  private val suggestAdapter = SuggestAdapter().apply { submitList(emptyList()) }
  private val topMonthAdapter = TopMonthAdapter().apply { submitList(emptyList()) }

  private val suggestRetryS = PublishRelay.create<Unit>()
  private val topMonthRetryS = PublishRelay.create<Unit>()
  private val updatedRetryS = PublishRelay.create<Unit>()
  private val clickComicS = PublishRelay.create<Comic>()

  val suggestRetryObservable = suggestRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)!!
  val topMonthRetryObservable = topMonthRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)!!
  val updatedRetryObservable = updatedRetryS.throttleFirst(500, TimeUnit.MILLISECONDS)!!
  val clickComicObservable = Observable.mergeArray(
    suggestAdapter.clickComicObservable,
    topMonthAdapter.clickComicObservable,
    clickComicS.asObservable()
  )!!

  override fun onCreateViewHolder(parent: ViewGroup, @ViewType viewType: Int): VH {
    return when (viewType) {
      SUGGEST_LIST_VIEW_TYPE -> SuggestListVH(parent inflate R.layout.item_recycler_home_recycler)
      TOP_MONTH_LIST_VIEW_TYPE -> TopMonthListVH(parent inflate R.layout.item_recycler_home_recycler)
      COMIC_ITEM_VIEW_TYPE -> ComicItemVH(
        parent inflate R.layout.item_recyclerview_updated_comic,
        parent
      )
      ERROR_ITEM_VIEW_TYPE -> ErrorVH(parent inflate R.layout.item_recyclerview_updated_error)
      LOADING_ITEM_VIEW_TYPE -> LoadingVH(parent inflate R.layout.item_recyclerview_updated_loading)
      HEADER_VIEW_TYPE -> HeaderVH(parent inflate R.layout.item_recycler_home_header)
      else -> throw IllegalStateException("Unknown view type: $viewType")
    }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  @ViewType
  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is HomeListItem.SuggestListState -> SUGGEST_LIST_VIEW_TYPE
      is HomeListItem.TopMonthListState -> TOP_MONTH_LIST_VIEW_TYPE
      is HomeListItem.UpdatedItem.ComicItem -> COMIC_ITEM_VIEW_TYPE
      is HomeListItem.UpdatedItem.Error -> ERROR_ITEM_VIEW_TYPE
      HomeListItem.UpdatedItem.Loading -> LOADING_ITEM_VIEW_TYPE
      is HomeListItem.Header -> HEADER_VIEW_TYPE
    }
  }

  abstract class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: HomeListItem)
  }

  private abstract class HorizontalRecyclerVH(itemView: View) : VH(itemView) {
    protected val recycler = itemView.home_recycler_horizontal!!
    protected val progressBar = itemView.home_progress_bar!!
    protected val textErrorMessage = itemView.home_error_message!!
    protected val buttonRetry = itemView.button_home_horizontal_retry!!
    protected val errorGroup = itemView.error_group!!
  }

  private inner class SuggestListVH(itemView: View) : HorizontalRecyclerVH(itemView),
    LifecycleObserver {
    private var currentList = emptyList<Comic>()

    private val startStopAutoScrollS = PublishRelay.create<Boolean>()
    private val intervalInMillis = 1_200L
    private val delayAfterTouchInMillis = 3_000L
    private var disposable: Disposable? = null

    init {
      buttonRetry.setOnClickListener { suggestRetryS.accept(Unit) }

      recycler.run {
        setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        layoutManager = linearLayoutManager
        adapter = suggestAdapter

        PagerSnapHelper().attachToRecyclerView(this)

        disposable = startStopAutoScrollS
          .mergeWith(
            scrollStateChanges()
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
          .map { it to suggestAdapter.itemCount }
          .doOnNext { Timber.d("[HORZ] auto_scroll=$it") }
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
              Timber.d(
                """[HORZ] scroll_to_position=$it, itemCount=${suggestAdapter.itemCount},
              | range=0..${suggestAdapter.itemCount - 1}""".trimMargin()
              )
              if (it >= 0) {
                smoothScrollToPosition(it.toInt())
              }
            },
            onError = {}
          )
      }

      lifecycleOwner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() = startStopAutoScrollS.accept(true).also { Timber.d("[HORZ] start") }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun onPause() = startStopAutoScrollS.accept(false).also { Timber.d("[HORZ] stop") }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() =
      disposable?.takeUnless { it.isDisposed }?.dispose().also { Timber.d("[HORZ] dipose") }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.SuggestListState>(item) { (comics, errorMessage, isLoading) ->
        Timber.d("suggest_state=[$isLoading, $errorMessage, $comics]")

        if (isLoading) {
          progressBar.visibility = View.VISIBLE
        } else {
          progressBar.visibility = View.INVISIBLE
        }

        if (errorMessage != null) {
          errorGroup.visibility = View.VISIBLE
          textErrorMessage.text = errorMessage
        } else {
          errorGroup.visibility = View.INVISIBLE
        }

        if (currentList != comics) {
          suggestAdapter.submitList(comics) {
            currentList = comics
            if (comics.isNotEmpty()) {
              recycler.scrollToPosition(0)
            }
            startStopAutoScrollS.accept(comics.size > 1)
          }
        }
      }
  }

  private inner class TopMonthListVH(itemView: View) : HorizontalRecyclerVH(itemView) {
    private var currentList = emptyList<Comic>()

    init {
      buttonRetry.setOnClickListener { topMonthRetryS.accept(Unit) }

      recycler.run {
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        adapter = topMonthAdapter
      }
    }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.TopMonthListState>(item) { (comics, errorMessage, isLoading) ->
        Timber.d("top_month_state=[$isLoading, $errorMessage, $comics]")

        if (isLoading) {
          progressBar.visibility = View.VISIBLE
        } else {
          progressBar.visibility = View.INVISIBLE
        }

        if (errorMessage != null) {
          errorGroup.visibility = View.VISIBLE
          textErrorMessage.text = errorMessage
        } else {
          errorGroup.visibility = View.INVISIBLE
        }

        if (currentList != comics) {
          topMonthAdapter.submitList(comics) { currentList = comics }
        }
      }
  }

  private inner class ComicItemVH(itemView: View, parent: ViewGroup) : VH(itemView) {
    internal val imageComic = itemView.image_comic!!
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
        .map { adapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .map { getItem(it) }
        .ofType<HomeListItem.UpdatedItem.ComicItem>()
        .map { it.comic }
        .subscribe(clickComicS::accept)
    }

    override fun bind(item: HomeListItem) =
      onlyBind<HomeListItem.UpdatedItem.ComicItem>(item) { (comic) ->
        GlideApp
          .with(itemView.context)
          .load(comic.thumbnail)
          .thumbnail(0.5f)
          .fitCenter()
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(imageComic)

        textComicName.text = comic.title
        textView.text = comic.view

        textChapters
          .zip(comic.chapters)
          .forEach { (textViews, chapter) ->
            textViews.first.text = chapter.chapterName
            textViews.second.text = chapter.time ?: "..."
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
        SUGGEST -> "Recommended"
        TOP_MONTH -> "Top month"
        UPDATED -> "Updated"
      }
    }
  }

  override fun onViewRecycled(holder: VH) {
    super.onViewRecycled(holder)
    if (holder is ComicItemVH) {
      GlideApp
        .with(holder.itemView.context)
        .clear(holder.imageComic)
      Timber.d("onViewRecycled")
    }
  }

  companion object {
    const val SUGGEST_LIST_VIEW_TYPE = 1
    const val TOP_MONTH_LIST_VIEW_TYPE = 2
    const val COMIC_ITEM_VIEW_TYPE = 3
    const val ERROR_ITEM_VIEW_TYPE = 4
    const val LOADING_ITEM_VIEW_TYPE = 5
    const val HEADER_VIEW_TYPE = 6

    @IntDef(
      value = [
        SUGGEST_LIST_VIEW_TYPE,
        TOP_MONTH_LIST_VIEW_TYPE,
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
      crossinline bind: (T) -> Unit
    ) {
      if (item !is T) {
        throw IllegalStateException("${this::class.java.simpleName}::bind only accept ${T::class.java.simpleName}, but item=$item")
      }
      bind(item)
    }
  }
}


