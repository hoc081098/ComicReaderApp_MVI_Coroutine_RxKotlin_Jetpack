package com.hoc.comicapp.ui.downloaded_comics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.SingleEvent
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewIntent
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_downloaded_comics.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class DownloadedComicsFragment : Fragment() {
  private val viewModel by viewModel<DownloadedComicsViewModel>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_downloaded_comics, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val adapter = DownloadedComicsAdapter(GlideApp.with(this))
    initView(adapter)
    bind(adapter)
  }

  private fun initView(downloadedComicsAdapter: DownloadedComicsAdapter) {
    recycler_comics.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = downloadedComicsAdapter
    }
  }

  private fun bind(adapter: DownloadedComicsAdapter) {
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, errorMessage, comics) ->
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
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is SingleEvent.Message -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.processIntents(
      Observable.just(ViewIntent.Initial)
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}