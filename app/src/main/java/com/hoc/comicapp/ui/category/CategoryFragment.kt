package com.hoc.comicapp.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract
import com.hoc.comicapp.utils.itemSelections
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding4.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_category.*
import org.koin.androidx.scope.lifecycleScope
import org.koin.androidx.viewmodel.scope.viewModel
import timber.log.Timber

class CategoryFragment : Fragment() {
  private val viewModel by lifecycleScope.viewModel<CategoryViewModel>(owner = this)
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View =
    inflater.inflate(R.layout.fragment_category, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val adapter = CategoryAdapter(GlideApp.with(this), compositeDisposable)
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
        val toCategoryDetailFragment =
          CategoryFragmentDirections.actionCategoryFragmentToCategoryDetailFragment(
            title = it.name,
            category = CategoryDetailContract.CategoryArg(
              description = it.description,
              link = it.link,
              name = it.name,
              thumbnail = it.thumbnail
            )
          )
        findNavController().navigate(toCategoryDetailFragment)
      }
      .addTo(compositeDisposable)

    spinner_sort_title.setItems(orders)
    spinner_sort_title.selectedIndex = viewModel.state
      .safeValue
      ?.sortOrder
      ?.let(orders::indexOf)
      ?: 0
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

      if (refreshLoading) {
        swipe_refresh_layout.post { swipe_refresh_layout.isRefreshing = true }
      } else {
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

  private companion object {
    val orders = listOf(CATEGORY_NAME_ASC, CATEGORY_NAME_DESC)
  }
}