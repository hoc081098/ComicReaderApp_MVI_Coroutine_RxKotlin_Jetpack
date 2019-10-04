package com.hoc.comicapp.ui.search_comic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.MainActivity
import com.hoc.comicapp.ui.search_comic.SearchComicContract.SingleEvent
import com.hoc.comicapp.ui.search_comic.SearchComicContract.ViewIntent
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_search_comic.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class SearchComicFragment : Fragment() {
  private val viewModel by viewModel<SearchComicViewModel>()
  private val compositeDisposable = CompositeDisposable()
  private val mainActivity get() = requireActivity() as MainActivity

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
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

  private fun initView(searchComicAdapter: SearchComicAdapter) {
    mainActivity.showSearch()

    recycler_search_comic.run {
      setHasFixedSize(true)
      layoutManager = GridLayoutManager(context, 2)
      adapter = searchComicAdapter
    }

    searchComicAdapter
      .clickComicObservable
      .subscribeBy {
        val toComicDetailFragment =
          SearchComicFragmentDirections.actionSearchComicFragmentToComicDetailFragment(
            comic = it,
            title = it.title
          )
        findNavController().navigate(toComicDetailFragment)
      }
      .addTo(compositeDisposable)
  }

  private fun bind(adapter: SearchComicAdapter) {
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

      adapter.submitList(comics)
    }
    viewModel.processIntents(
      Observable.mergeArray(
        mainActivity
          .textSearchChanges()
          .map { ViewIntent.SearchIntent(it) },
        button_retry
          .clicks()
          .map { ViewIntent.RetryIntent }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    mainActivity.hideSearchIfNeeded()
    compositeDisposable.clear()
  }
}