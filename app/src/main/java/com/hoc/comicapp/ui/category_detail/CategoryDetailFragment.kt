package com.hoc.comicapp.ui.category_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewIntent
import com.hoc.comicapp.ui.category_detail.CategoryDetailFragmentDirections.Companion.actionCategoryDetailFragmentToComicDetailFragment
import com.hoc.comicapp.ui.home.ComicArg
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.observe
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.Observable.just
import io.reactivex.Observable.mergeArray
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_category_detail.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class CategoryDetailFragment : Fragment() {
  private val args by navArgs<CategoryDetailFragmentArgs>()

  private val vm by viewModel<CategoryDetailVM>() { parametersOf(args.category) }
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(R.layout.fragment_category_detail, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val categoryDetailAdapter = CategoryDetailAdapter(
      GlideApp.with(this),
      viewLifecycleOwner,
      compositeDisposable,
      ::onClickComic
    )

    initView(categoryDetailAdapter)
    bindVM(categoryDetailAdapter)
  }

  private fun bindVM(categoryDetailAdapter: CategoryDetailAdapter) {
    vm.state.observe(owner = viewLifecycleOwner) { (items, isRefreshing) ->
      categoryDetailAdapter.submitList(items)
      if (isRefreshing) {
        swipe_refresh_layout.post { swipe_refresh_layout.isRefreshing = true }
      } else {
        swipe_refresh_layout.isRefreshing = false
      }
    }
    vm.processIntents(
      mergeArray(
        just(ViewIntent.Initial(args.category)),
        loadNextPageIntent(),
        swipe_refresh_layout.refreshes().map { ViewIntent.Refresh },
        categoryDetailAdapter.retryObservable.map { ViewIntent.Retry },
        categoryDetailAdapter.retryPopularObservable.map { ViewIntent.RetryPopular }
      )
    ).addTo(compositeDisposable)
  }

  private fun initView(categoryDetailAdapter: CategoryDetailAdapter) {
    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(R.array.swipe_refresh_colors))

    recycler_category_detail.run {
      layoutManager = GridLayoutManager(context, 2).apply {
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
        override fun getVerticalSnapPreference() = SNAP_TO_START
      }.apply { targetPosition = 0 }.let { recycler_category_detail.layoutManager!!.startSmoothScroll(it) }
    }

    recycler_category_detail
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

  private fun loadNextPageIntent(): Observable<ViewIntent.LoadNextPage> {
    return recycler_category_detail
      .scrollEvents()
      .filter { (_, _, dy) ->
        val gridLayoutManager = recycler_category_detail.layoutManager as GridLayoutManager
        dy > 0 && gridLayoutManager.findLastVisibleItemPosition() + 2 * maxSpanCount >= gridLayoutManager.itemCount
      }
      .map { ViewIntent.LoadNextPage }
  }

  private val maxSpanCount get() = if (requireContext().isOrientationPortrait) 2 else 4

  private fun onClickComic(comic: ComicArg) {
    val toComicDetailFragment = actionCategoryDetailFragmentToComicDetailFragment(
      title = comic.title,
      isDownloaded = false,
      comic = comic
    )
    findNavController().navigate(toComicDetailFragment)
  }
}