package com.hoc.comicapp.ui.downloaded_comics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.SingleEvent
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewIntent
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.toast
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.koin.androidx.viewmodel.ext.android.viewModel

class DownloadedComicsFragment : Fragment() {
  private val viewModel by viewModel<DownloadedComicsViewModel>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_downloaded_comics, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bind()
  }

  private fun bind() {
    viewModel.state.observe(owner = viewLifecycleOwner) {
      requireContext().toast(it.toString())
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