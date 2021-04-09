package com.hoc.comicapp.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.FragmentCategoryBinding
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.utils.itemSelections
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class CategoryFragment : ScopeFragment() {
  private val viewModel by viewModel<CategoryViewModel>()
  private val viewBinding by viewBinding<FragmentCategoryBinding>()
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

  private fun initView(categoryAdapter: CategoryAdapter) = viewBinding.run {
    recyclerCategories.run {
      setHasFixedSize(true)
      layoutManager = StaggeredGridLayoutManager(2, VERTICAL)
      adapter = categoryAdapter
    }

    categoryAdapter.clickCategoryObservable
      .subscribeBy {
        val toCategoryDetailFragment =
          CategoryFragmentDirections.actionCategoryFragmentToCategoryDetailFragment(
            title = it.name,
            category = Arguments.CategoryDetailArgs(
              description = it.description,
              link = it.link,
              name = it.name,
              thumbnail = it.thumbnail
            )
          )
        requireAppNavigator.execute { navigate(toCategoryDetailFragment) }
      }
      .addTo(compositeDisposable)

    spinnerSortTitle.setItems(orders)
    spinnerSortTitle.selectedIndex = viewModel.state
      .value
      .sortOrder
      .let(orders::indexOf)
  }

  private fun bind(adapter: CategoryAdapter) = viewBinding.run {
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
        swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }
      } else {
        swipeRefreshLayout.isRefreshing = false
      }

      if (isLoading) {
        progressBar.visibility = View.VISIBLE
      } else {
        progressBar.visibility = View.INVISIBLE
      }

      if (errorMessage == null) {
        groupError.visibility = View.GONE
      } else {
        groupError.visibility = View.VISIBLE
        textErrorMessage.text = errorMessage
      }

      adapter.submitList(categories)
    }
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(CategoryViewIntent.Initial),
        buttonRetry
          .clicks()
          .map { CategoryViewIntent.Retry },
        swipeRefreshLayout
          .refreshes()
          .map { CategoryViewIntent.Refresh },
        spinnerSortTitle
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
