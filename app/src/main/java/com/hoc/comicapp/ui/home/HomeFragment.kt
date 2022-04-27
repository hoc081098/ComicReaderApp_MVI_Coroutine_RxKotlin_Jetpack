package com.hoc.comicapp.ui.home

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.base.BaseFragment
import com.hoc.comicapp.databinding.FragmentHomeBinding
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.unit
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.recyclerview.scrollEvents
import com.jakewharton.rxbinding4.swiperefreshlayout.refreshes
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class HomeFragment :
  BaseFragment<
    HomeViewIntent,
    HomeViewState,
    HomeSingleEvent,
    HomeViewModel,
    >(R.layout.fragment_home) {
  override val viewModel by viewModel<HomeViewModel>()
  override val viewBinding by viewBinding<FragmentHomeBinding> {
    recyclerHome.adapter = null
  }

  private val homeAdapter by lazy(NONE) {
    HomeAdapter(
      viewLifecycleOwner,
      GlideApp.with(this),
      viewBinding.recyclerHome.recycledViewPool,
      compositeDisposable
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    exitTransition = Hold().apply {
      duration = resources.getInteger(R.integer.reply_motion_default_large).toLong()
    }
  }

  private fun getMaxSpanCount() = if (requireContext().isOrientationPortrait) 2 else 4

  /*
   *
   */

  override fun setupView(view: View, savedInstanceState: Bundle?) {
    // Transition
    postponeEnterTransition()
    view.doOnPreDraw { startPostponedEnterTransition() }

    // Swipe refresh layout and recycler view
    viewBinding.swipeRefreshLayout.setColorSchemeColors(*resources.getIntArray(R.array.swipe_refresh_colors))
    viewBinding.recyclerHome.run {
      setHasFixedSize(true)
      layoutManager = GridLayoutManager(this@HomeFragment.context, getMaxSpanCount()).apply {
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
      adapter = homeAdapter.apply { lifecycleOwner = viewLifecycleOwner }

      addOnItemTouchListener(
        object : RecyclerView.SimpleOnItemTouchListener() {
          override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            if (e.action == MotionEvent.ACTION_DOWN &&
              rv.scrollState == RecyclerView.SCROLL_STATE_SETTLING
            ) {
              Timber.d("Stop scroll")
              rv.stopScroll()
            }
            return false
          }
        }
      )
    }

    // Setup fab
    val scroller = object : LinearSmoothScroller(view.context) {
      override fun getVerticalSnapPreference() = SNAP_TO_START
    }
    viewBinding.fab.setOnClickListener {
      scroller
        .apply { targetPosition = 0 }
        .let { viewBinding.recyclerHome.layoutManager!!.startSmoothScroll(it) }
    }
    viewBinding.recyclerHome
      .scrollEvents()
      .subscribeBy {
        if (it.dy < 0) {
          viewBinding.fab.show()
        } else {
          viewBinding.fab.hide()
        }
      }
      .addTo(compositeDisposable)

    // Adapter click event
    homeAdapter
      .clickComicObservable
      .subscribeBy { (view, comicArg, transitionName) ->
        val toComicDetailFragment =
          HomeFragmentDirections.actionHomeFragmentDestToComicDetailFragment(
            comic = comicArg,
            title = comicArg.title,
            isDownloaded = false,
            transitionName = transitionName
          )

        view.transitionName = transitionName
        val extras = FragmentNavigatorExtras(view to view.transitionName)

        requireAppNavigator.execute { navigate(toComicDetailFragment, extras) }
      }
      .addTo(compositeDisposable)
  }

  override fun viewIntents(): Observable<HomeViewIntent> {
    fun loadNextPageIntent(): Observable<HomeViewIntent.LoadNextPageUpdatedComic> {
      return viewBinding.recyclerHome
        .scrollEvents()
        .filter { (_, _, dy) ->
          val gridLayoutManager = viewBinding.recyclerHome.layoutManager as GridLayoutManager
          dy > 0 && gridLayoutManager.findLastVisibleItemPosition() + 2 * getMaxSpanCount() >= gridLayoutManager.itemCount
        }
        .map { HomeViewIntent.LoadNextPageUpdatedComic }
    }

    return Observable.mergeArray(
      Observable.just(HomeViewIntent.Initial),
      viewBinding.swipeRefreshLayout.refreshes().map { HomeViewIntent.Refresh },
      loadNextPageIntent(),
      homeAdapter.newestRetryObservable.map { HomeViewIntent.RetryNewest },
      homeAdapter.mostViewedRetryObservable.map { HomeViewIntent.RetryMostViewed },
      homeAdapter.updatedRetryObservable.map { HomeViewIntent.RetryUpdate }
    )
  }

  override fun render(viewState: HomeViewState) {
    val (items, refreshLoading) = viewState
    Timber.d("state=${items.size} refreshLoading=$refreshLoading")

    homeAdapter.submitList(items)

    viewBinding.swipeRefreshLayout.run {
      if (refreshLoading) {
        if (!isRefreshing) {
          post { isRefreshing = true }
        }
      } else {
        isRefreshing = false
      }
    }
  }

  override fun handleEvent(event: HomeSingleEvent) {
    return when (event) {
      is HomeSingleEvent.MessageEvent -> {
        view?.snack(event.message)
      }
    }.unit
  }
}
