package com.hoc.comicapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.*
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.hoc.comicapp.*
import com.hoc.comicapp.data.models.Comic
import com.jakewharton.rxbinding3.recyclerview.scrollStateChanges
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.item_recyclerview_top_month_comic_or_recommened.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ComicDiffUtilItemCallback : DiffUtil.ItemCallback<Comic>() {
  override fun areItemsTheSame(oldItem: Comic, newItem: Comic): Boolean {
    return oldItem.link == newItem.link
  }

  override fun areContentsTheSame(oldItem: Comic, newItem: Comic): Boolean {
    return oldItem == newItem
  }
}

class TopMonthAdapter :
  ListAdapter<Comic, TopMonthAdapter.VH>(ComicDiffUtilItemCallback()) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return LayoutInflater.from(parent.context)
      .inflate(R.layout.item_recyclerview_top_month_comic_or_recommened, parent, false)
      .let(::VH)
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!
    private val textView = itemView.text_view_or_last_updated_time!!

    fun bind(item: Comic) {
      textComicName.text = item.title
      textChapter.text = item.chapters.first().chapterName
      textView.text = item.view

      GlideApp
        .with(itemView.context)
        .load(item.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(withCrossFade())
        .into(imageComic)
    }
  }
}

class SuggestAdapter :
  ListAdapter<Comic, SuggestAdapter.VH>(ComicDiffUtilItemCallback()) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return LayoutInflater.from(parent.context)
      .inflate(R.layout.item_recyclerview_top_month_comic_or_recommened, parent, false)
      .let(::VH)
  }

  override fun onBindViewHolder(holder: VH, position: Int) =
    holder.bind(getItem(position % itemCount))

  class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!
    private val textLastUpdatedTime = itemView.text_view_or_last_updated_time!!
    private val imageIconClock = itemView.image_eye_or_clock!!

    fun bind(item: Comic) {
      textComicName.text = item.title
      textChapter.text = item.chapters.first().chapterName
      textLastUpdatedTime.text = item.chapters.first().time
      imageIconClock.setImageResource(R.drawable.ic_access_time_white_24dp)

      GlideApp
        .with(itemView.context)
        .load(item.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(withCrossFade())
        .into(imageComic)
    }
  }
}

@ExperimentalCoroutinesApi
class HomeFragment : Fragment() {
  private val homeViewModel by viewModel<HomeViewModel>()
  private val topMonthAdapter = TopMonthAdapter()
  private val suggestAdapter = SuggestAdapter()
  private val compositeDisposableDisposeOnPause = CompositeDisposable()
  private val compositeDisposableDisposeOnDestroyView = CompositeDisposable()
  private val startStopAutoScrollRelaySubject = PublishRelay.create<Boolean>()
  private val intervalInMillis = 1_200L

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_home, container, false).also {
    Timber.d("HomeFragment::onCreateView")
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("HomeFragment::onViewCreated")

    initView()
    observeViewModel(view)
  }

  private fun observeViewModel(view: View) {
    homeViewModel.state.observe(this) {
      updateTopMonth(it)
      updateSuggest(it)
    }
    homeViewModel.singleEvent.observeEvent(this) {
      when (it) {
        is HomeSingleEvent.MessageEvent -> {
          view.snack(it.message)
        }
      }
    }
  }

  private fun initView() {
    recycler_top_month.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
      adapter = topMonthAdapter
    }

    recycler_suggest.run {
      setHasFixedSize(true)
      val linearLayoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
      layoutManager = linearLayoutManager
      adapter = suggestAdapter

      PagerSnapHelper().attachToRecyclerView(this)

      startStopAutoScrollRelaySubject.mergeWith(
        scrollStateChanges()
          .filter { it == RecyclerView.SCROLL_STATE_DRAGGING }
          .switchMap {
            Observable.just(false).concatWith(
              Observable.timer(
                3_000,
                TimeUnit.MILLISECONDS
              ).map { true }
            )
          }
      ).doOnNext { Timber.d("auto_scroll=$it") }
        .switchMap { startAutoScroll ->
          val itemCount = suggestAdapter.itemCount

          if (!startAutoScroll || itemCount == 0) {
            Observable.just(-1L)
          } else {
            Observable
              .interval(intervalInMillis, TimeUnit.MILLISECONDS)
              .map { it % suggestAdapter.itemCount }
          }
        }
        .observeOn(AndroidSchedulers.mainThread()).subscribeBy(
          onNext = {
            Timber.d(
              """scroll_to_position=$it, itemCount=${suggestAdapter.itemCount},
              | range=0..${suggestAdapter.itemCount - 1}""".trimMargin()
            )
            if (it >= 0) {
              smoothScrollToPosition(it.toInt())
            }
          },
          onError = { requireContext().toast("Error ${it.message}") }
        ).addTo(compositeDisposableDisposeOnDestroyView)
    }

    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(R.array.swipe_refresh_colors))
  }

  override fun onResume() {
    super.onResume()
    Timber.d("HomeFragment::onResume")

    startStopAutoScrollRelaySubject.accept(true)
    homeViewModel.processIntents(
      Observable.mergeArray(
        Observable.just(HomeViewIntent.Initial),
        swipe_refresh_layout.refreshes().map { HomeViewIntent.Refresh }
      )
    ).addTo(compositeDisposableDisposeOnPause)
  }

  override fun onPause() {
    super.onPause()
    Timber.d("HomeFragment::onPause")

    startStopAutoScrollRelaySubject.accept(false)
    compositeDisposableDisposeOnPause.clear()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("HomeFragment::onDestroyView")

    compositeDisposableDisposeOnDestroyView.clear()
  }

  private fun updateTopMonth(state: HomeViewState) {
    Timber.d("top_month_state=[${state.topMonthLoading}, ${state.topMonthErrorMessage}, ${state.topMonthComics}]")

    if (state.topMonthLoading) {
      top_month_progress_bar.visibility = View.VISIBLE
    } else {
      top_month_progress_bar.visibility = View.INVISIBLE
    }

    if (state.topMonthErrorMessage != null) {
      top_month_error_message.visibility = View.VISIBLE
      top_month_error_message.text = state.topMonthErrorMessage
    } else {
      top_month_error_message.visibility = View.INVISIBLE
    }

    topMonthAdapter.submitList(state.topMonthComics)
  }

  private fun updateSuggest(state: HomeViewState) {
    Timber.d("suggest_state=[${state.suggestLoading}, ${state.suggestErrorMessage}, ${state.suggestComics}]")

    if (state.suggestLoading) {
      suggest_progress_bar.visibility = View.VISIBLE
    } else {
      suggest_progress_bar.visibility = View.INVISIBLE
    }

    if (state.suggestErrorMessage != null) {
      suggest_error_message.visibility = View.VISIBLE
      suggest_error_message.text = state.suggestErrorMessage
    } else {
      suggest_error_message.visibility = View.INVISIBLE
    }

    suggestAdapter.submitList(state.suggestComics, Runnable {
      if (state.suggestComics.isNotEmpty()) {
        recycler_suggest.scrollToPosition(1)
      }
      startStopAutoScrollRelaySubject.accept(state.suggestComics.size > 1)
    })
  }
}
