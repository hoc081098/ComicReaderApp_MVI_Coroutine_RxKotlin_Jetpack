package com.hoc.comicapp.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.*
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_category.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class CategoryFragment : Fragment() {
  private val viewModel by viewModel<CategoryViewModel>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    inflater.inflate(R.layout.fragment_category, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val adapter = CategoryAdapter(GlideApp.with(this))
    initView(adapter)
    bind(adapter)
  }

  private fun initView(categoryAdapter: CategoryAdapter) {
    recycler_categories.run {
      setHasFixedSize(true)
      layoutManager = StaggeredGridLayoutManager(2, VERTICAL)
      adapter = categoryAdapter
    }

    categoryAdapter.clickCategoryObservable
      .subscribeBy {
        context?.toast("Click $it")
      }
      .addTo(compositeDisposable)

    spinner_sort_title.setItems(listOf(CATEGORY_NAME_ASC, CATEGORY_NAME_DESC))
  }

  private fun bind(adapter: CategoryAdapter) {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is CategorySingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, categories, errorMessage, refreshLoading) ->
      Timber.d("[STATE] categories.length=${categories.size} isLoading=$isLoading errorMessage=$errorMessage")

      if (!refreshLoading) {
        swipe_refresh_layout.isRefreshing = false
      }

      if (isLoading) {
        progress_bar.visibility = View.VISIBLE
      } else {
        progress_bar.visibility = View.INVISIBLE
      }

      if (errorMessage == null) {
        group_error.visibility = View.GONE
      } else {
        group_error.visibility = View.VISIBLE
        text_error_message.text = errorMessage
      }

      adapter.submitList(categories)
    }
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(CategoryViewIntent.Initial),
        button_retry
          .clicks()
          .map { CategoryViewIntent.Retry },
        swipe_refresh_layout
          .refreshes()
          .map { CategoryViewIntent.Refresh },
        spinner_sort_title
          .itemSelections<String>()
          .map { CategoryViewIntent.ChangeSortOrder(it) }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    compositeDisposable.clear()
  }
}