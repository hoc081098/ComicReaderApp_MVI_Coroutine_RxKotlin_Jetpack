package com.hoc.comicapp.ui.search_comic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hoc.comicapp.MainActivity
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.textChanges
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_search_comic.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchComicFragment : Fragment() {
  private val viewModel by viewModel<SearchComicViewModel>()
  private val compositeDisposable = CompositeDisposable()
  private val mainActivity get() = requireActivity() as MainActivity

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    inflater.inflate(R.layout.fragment_search_comic, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    mainActivity.showSearch()
    bind()
  }

  private fun bind() {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is SearchComicSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.state.observe(owner = viewLifecycleOwner) {
      texttttt.text = it.toString()
    }
    viewModel.processIntents(
      Observable.mergeArray(
        mainActivity
          .search_view
          .textChanges()
          .map { SearchComicViewIntent.SearchIntent(it) }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}