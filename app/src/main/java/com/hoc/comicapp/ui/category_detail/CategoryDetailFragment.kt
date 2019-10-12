package com.hoc.comicapp.ui.category_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewIntent
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewState.Item.Comic
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.observe
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.Observable.just
import io.reactivex.Observable.mergeArray
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_category_detail.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

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
    }

    vm.state.observe(owner = viewLifecycleOwner) { (items, isRefreshing) ->
      categoryDetailAdapter.submitList(items)
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

  private fun onClickComic(comic: Comic) {

  }
}