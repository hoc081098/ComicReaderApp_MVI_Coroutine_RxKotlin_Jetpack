package com.hoc.comicapp.ui.downloading_chapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.*
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_downloading_chapters.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel

@ExperimentalCoroutinesApi
class DownloadingChaptersFragment : Fragment() {
  private val viewModel by viewModel<DownloadingChaptersViewModel>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_downloading_chapters, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val adapter = DownloadingChaptersAdapter(GlideApp.with(this))
    initView(adapter)
    bind(adapter)
  }

  private fun initView(chaptersAdapter: DownloadingChaptersAdapter) {
    recycler_chapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chaptersAdapter
    }
  }

  private fun bind(adapter: DownloadingChaptersAdapter) {
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, errorMessage, comics) ->
      if (isLoading) {
        progress_bar.visibility = View.VISIBLE
      } else {
        progress_bar.visibility = View.INVISIBLE
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
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}