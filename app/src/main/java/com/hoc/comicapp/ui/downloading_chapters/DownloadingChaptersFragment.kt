package com.hoc.comicapp.ui.downloading_chapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.FragmentDownloadingChaptersBinding
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.SingleEvent
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewIntent
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState.Chapter
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.showAlertDialogAsMaybe
import com.hoc.comicapp.utils.snack
import com.hoc081098.viewbindingdelegate.viewBinding
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DownloadingChaptersFragment : ScopeFragment() {
  private val viewModel by viewModel<DownloadingChaptersViewModel>()
  private val viewBinding by viewBinding<FragmentDownloadingChaptersBinding>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.fragment_downloading_chapters, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("DownloadingChaptersFragment::onViewCreated")

    val adapter = DownloadingChaptersAdapter(
      compositeDisposable
    )
    initView(adapter)
    bind(adapter)
  }

  private fun initView(chaptersAdapter: DownloadingChaptersAdapter) = viewBinding.run {
    recyclerChapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chaptersAdapter
    }
  }

  private fun bind(adapter: DownloadingChaptersAdapter) = viewBinding.run {
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, errorMessage, chapters) ->
      progressBar.isVisible = isLoading
      emptyLayout.isVisible = chapters.isEmpty()
      adapter.submitList(chapters)
      Timber.d("DownloadingChaptersFragment::state $isLoading $errorMessage ${chapters.size}")
    }
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is SingleEvent.Message -> {
          view?.snack(it.message)
        }
        is SingleEvent.Deleted -> {
          view?.snack("Cancel downloading ${it.chapter.title}")
        }
        is SingleEvent.DeleteError -> {
          view?.snack("Error when canceling downloading ${it.chapter.title}: ${it.error.getMessage()}")
        }
      }
    }
    viewModel
      .processIntents(
        Observable.mergeArray(
          Observable.just(ViewIntent.Initial),
          adapter
            .clickCancel
            .exhaustMap(::showCancelDownloadingDialog)
            .map { ViewIntent.CancelDownload(it) }
        )
      )
      .addTo(compositeDisposable)
  }

  private fun showCancelDownloadingDialog(chapter: Chapter): Observable<Chapter> {
    return requireActivity()
      .showAlertDialogAsMaybe {
        title("Cancel downloading")
        message("This chapter won't be available to read offline")
        cancelable(true)
        iconId(R.drawable.ic_delete_white_24dp)
      }
      .map { chapter }
      .toObservable()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
    Timber.d("DownloadingChaptersFragment::onDestroyView")
  }
}
