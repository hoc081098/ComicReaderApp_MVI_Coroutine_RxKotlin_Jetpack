package com.hoc.comicapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

@ExperimentalCoroutinesApi
class HomeFragment : Fragment() {
  private val homeViewModel by viewModel<HomeViewModel>()
  private val compositeDisposable = CompositeDisposable()

  private val homeAdapter by lazy(NONE) {
    HomeAdapter(
      viewLifecycleOwner,
      GlideApp.with(this),
      recycler_home.recycledViewPool,
      compositeDisposable
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_home, container, false)
    .also { Timber.d("HomeFragment::onCreateView") }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("HomeFragment::onViewCreated")

    initView(homeAdapter)
    bind(homeAdapter)
  }

  private fun bind(homeAdapter: HomeAdapter) {
    homeViewModel.state.observe(owner = viewLifecycleOwner) { (items, refreshLoading) ->
      Timber.d("state=${items.size} refreshLoading=$refreshLoading")

      homeAdapter.submitList(items)
      if (refreshLoading) {
        swipe_refresh_layout.post { swipe_refresh_layout.isRefreshing = true }
      } else {
        swipe_refresh_layout.isRefreshing = false
      }
    }
    homeViewModel.singleEvent.observeEvent(viewLifecycleOwner) {
      when (it) {
        is HomeSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    homeViewModel.processIntents(
      Observable.mergeArray(
        Observable.just(HomeViewIntent.Initial),
        swipe_refresh_layout.refreshes().map { HomeViewIntent.Refresh },
        loadNextPageIntent(),
        homeAdapter.newestRetryObservable.map { HomeViewIntent.RetryNewest },
        homeAdapter.mostViewedRetryObservable.map { HomeViewIntent.RetryMostViewed },
        homeAdapter.updatedRetryObservable.map { HomeViewIntent.RetryUpdate }
      )
    ).addTo(compositeDisposable)
  }

  private fun initView(homeAdapter: HomeAdapter) {
    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(R.array.swipe_refresh_colors))

    recycler_home.run {

      setHasFixedSize(true)
      layoutManager = GridLayoutManager(context, getMaxSpanCount()).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
          override fun getSpanSize(position: Int): Int {
            return if (homeAdapter.getItemViewType(position) == HomeAdapter.COMIC_ITEM_VIEW_TYPE) {
              1
            } else {
              getMaxSpanCount()
            }
          }
        }
      }
      adapter = homeAdapter

      addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
          if (e.action == MotionEvent.ACTION_DOWN &&
            rv.scrollState == RecyclerView.SCROLL_STATE_SETTLING
          ) {
            Timber.d("Stop scroll")
            rv.stopScroll()
          }
          return false
        }
      })
    }

    fab.setOnClickListener {
      object : LinearSmoothScroller(it.context) {
        override fun getVerticalSnapPreference() = LinearSmoothScroller.SNAP_TO_START
      }.apply { targetPosition = 0 }.let { recycler_home.layoutManager!!.startSmoothScroll(it) }
    }

    recycler_home
      .scrollEvents()
      .subscribeBy {
        if (it.dy < 0) {
          fab.show()
        } else {
          fab.hide()
        }
      }
      .addTo(compositeDisposable)

    homeAdapter
      .clickComicObservable
      .subscribeBy {
        val toComicDetailFragment =
          HomeFragmentDirections.actionHomeFragmentDestToComicDetailFragment(
            comic = it,
            title = it.title,
            isDownloaded = false
          )
        findNavController().navigate(toComicDetailFragment)
      }
      .addTo(compositeDisposable)
  }

  private fun loadNextPageIntent(): Observable<HomeViewIntent.LoadNextPageUpdatedComic> {
    return recycler_home
      .scrollEvents()
      .filter { (_, _, dy) ->
        val gridLayoutManager = recycler_home.layoutManager as GridLayoutManager
        dy > 0 && gridLayoutManager.findLastVisibleItemPosition() + 2 * getMaxSpanCount() >= gridLayoutManager.itemCount
      }
      .map { HomeViewIntent.LoadNextPageUpdatedComic }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("HomeFragment::onDestroyView")
    compositeDisposable.clear()
    recycler_home.adapter = null
  }

  private fun getMaxSpanCount() = if (requireContext().isOrientationPortrait) 2 else 4
}
