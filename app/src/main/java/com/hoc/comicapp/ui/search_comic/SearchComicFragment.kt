package com.hoc.comicapp.ui.search_comic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.main.MainActivity
import com.hoc.comicapp.databinding.FragmentSearchComicBinding
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.ui.search_comic.SearchComicContract.SingleEvent
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewIntent
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.view.clicks
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SearchComicFragment : ScopeFragment() {
  private val viewModel by viewModel<SearchComicViewModel>()
  private val viewBinding by viewBinding<FragmentSearchComicBinding>()
  private val compositeDisposable = CompositeDisposable()
  private val mainActivity get() = requireActivity() as MainActivity

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View =
    inflater.inflate(R.layout.fragment_search_comic, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val searchComicAdapter = SearchComicAdapter(
      GlideApp.with(this),
      compositeDisposable
    )
    initView(searchComicAdapter)
    bind(searchComicAdapter)
  }

  private fun initView(searchComicAdapter: SearchComicAdapter) = viewBinding.run {
    view?.post { mainActivity.showSearch() }

    val maxSpanCount = if (requireContext().isOrientationPortrait) 2 else 4
    recyclerSearchComic.run {
      setHasFixedSize(true)
      layoutManager = GridLayoutManager(context, maxSpanCount).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
          override fun getSpanSize(position: Int): Int {
            return when (searchComicAdapter.getItemViewType(position)) {
              R.layout.item_recycler_search_comic_load_more -> maxSpanCount
              else -> 1
            }
          }
        }
      }
      adapter = searchComicAdapter
    }

    searchComicAdapter
      .clickComicObservable
      .subscribeBy {
        val toComicDetailFragment =
          SearchComicFragmentDirections.actionSearchComicFragmentToComicDetailFragment(
            comic = it,
            title = it.title,
            isDownloaded = false
          )
        requireAppNavigator.execute {
          navigate(toComicDetailFragment)
        }
      }
      .addTo(compositeDisposable)
  }

  private fun bind(adapter: SearchComicAdapter) = viewBinding.run {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is SingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, comics, errorMessage) ->
      Timber.d("[STATE] comics.length=${comics.size} isLoading=$isLoading errorMessage=$errorMessage")

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

      adapter.submitList(comics)

      emptyLayout.isVisible = !isLoading && errorMessage === null && comics.isEmpty()
    }
    viewModel.processIntents(
      Observable.mergeArray(
        mainActivity
          .textSearchChanges()
          .map { ViewIntent.SearchIntent(it) },
        buttonRetry
          .clicks()
          .map { ViewIntent.RetryFirstIntent },
        adapter
          .clickButtonRetryOrLoadMoreObservable
          .map { if (it) ViewIntent.RetryNextPage else ViewIntent.LoadNextPage }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    mainActivity.hideSearchIfNeeded()
    compositeDisposable.clear()
  }
}
