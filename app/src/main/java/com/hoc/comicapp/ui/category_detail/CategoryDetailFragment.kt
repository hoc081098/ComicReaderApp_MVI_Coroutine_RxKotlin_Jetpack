package com.hoc.comicapp.ui.category_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.FragmentCategoryDetailBinding
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewIntent
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.observe
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.recyclerview.scrollEvents
import com.jakewharton.rxbinding4.swiperefreshlayout.refreshes
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observable.just
import io.reactivex.rxjava3.core.Observable.mergeArray
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class CategoryDetailFragment : ScopeFragment() {
  private val args by navArgs<CategoryDetailFragmentArgs>()

  private val vm by viewModel<CategoryDetailVM> { parametersOf(args.category) }
  private val viewBinding by viewBinding<FragmentCategoryDetailBinding> {
    recyclerCategoryDetail.adapter = null
  }
  private val compositeDisposable = CompositeDisposable()

  private val categoryDetailAdapter by lazy(NONE) {
    CategoryDetailAdapter(
      GlideApp.with(this),
      viewLifecycleOwner,
      compositeDisposable,
      ::onClickComic
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ) = inflater.inflate(R.layout.fragment_category_detail, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initView(categoryDetailAdapter)
    bindVM(categoryDetailAdapter)
  }

  private fun bindVM(categoryDetailAdapter: CategoryDetailAdapter) = viewBinding.run {
    vm.state.observe(owner = viewLifecycleOwner) { (items, isRefreshing) ->
      categoryDetailAdapter.submitList(items)
      if (isRefreshing) {
        swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }
      } else {
        swipeRefreshLayout.isRefreshing = false
      }
    }
    vm.processIntents(
      mergeArray(
        just(ViewIntent.Initial(args.category)),
        loadNextPageIntent(),
        swipeRefreshLayout.refreshes().map { ViewIntent.Refresh },
        categoryDetailAdapter.retryObservable.map { ViewIntent.Retry },
        categoryDetailAdapter.retryPopularObservable.map { ViewIntent.RetryPopular }
      )
    ).addTo(compositeDisposable)
  }

  private fun initView(categoryDetailAdapter: CategoryDetailAdapter) = viewBinding.run {
    swipeRefreshLayout.setColorSchemeColors(*resources.getIntArray(R.array.swipe_refresh_colors))

    recyclerCategoryDetail.run {
      layoutManager = GridLayoutManager(context, maxSpanCount).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
          override fun getSpanSize(position: Int): Int {
            return if (categoryDetailAdapter.getItemViewType(position) == R.layout.item_recycler_category_detail_comic) {
              1
            } else {
              maxSpanCount
            }
          }
        }
      }
      setHasFixedSize(true)
      adapter = categoryDetailAdapter

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

    fab.setOnClickListener { view ->
      object : LinearSmoothScroller(view.context) {
        override fun getVerticalSnapPreference() = SNAP_TO_START
      }
        .apply { targetPosition = 0 }
        .let { recyclerCategoryDetail.layoutManager!!.startSmoothScroll(it) }
    }

    recyclerCategoryDetail
      .scrollEvents()
      .subscribeBy {
        if (it.dy < 0) {
          fab.show()
        } else {
          fab.hide()
        }
      }
      .addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }

  private fun loadNextPageIntent(): Observable<ViewIntent.LoadNextPage> = viewBinding.run {
    recyclerCategoryDetail
      .scrollEvents()
      .filter { (_, _, dy) ->
        val gridLayoutManager = recyclerCategoryDetail.layoutManager as GridLayoutManager
        dy > 0 && gridLayoutManager.findLastVisibleItemPosition() + 2 * maxSpanCount >= gridLayoutManager.itemCount
      }
      .map { ViewIntent.LoadNextPage }
  }

  private val maxSpanCount get() = if (requireContext().isOrientationPortrait) 2 else 4

  private fun onClickComic(comic: Arguments.ComicDetailArgs) {
    val toComicDetailFragment =
      CategoryDetailFragmentDirections.actionCategoryDetailFragmentToComicDetailFragment(
        title = comic.title,
        isDownloaded = false,
        comic = comic
      )
    requireAppNavigator.execute { navigate(toComicDetailFragment) }
  }
}
