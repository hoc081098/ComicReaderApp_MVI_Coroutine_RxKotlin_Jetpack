package com.hoc.comicapp.ui.downloading_chapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.SingleEvent
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewIntent
import com.hoc.comicapp.ui.downloading_chapters.DownloadingChaptersContract.ViewState.Chapter
import com.hoc.comicapp.utils.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_downloading_chapters.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@ExperimentalCoroutinesApi
class DownloadingChaptersFragment : Fragment() {
  private val viewModel by viewModel<DownloadingChaptersViewModel>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_downloading_chapters, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("DownloadingChaptersFragment::onViewCreated")

    val adapter = DownloadingChaptersAdapter(
      GlideApp.with(this),
      compositeDisposable
    )
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
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, errorMessage, chapters) ->
      progress_bar.isVisible = isLoading
      empty_layout.isVisible = chapters.isEmpty()
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
    return Observable.create<Chapter> { emitter ->
      val alertDialog = requireActivity().showAlertDialog {
        title("Cancel downloading")
        message("This chapter won't be available to read offline")
        cancelable(true)
        iconId(R.drawable.ic_delete_white_24dp)

        negativeAction("Cancel") { dialog, _ ->
          dialog.cancel()
          if (!emitter.isDisposed) {
            emitter.onComplete()
          }
        }
        positiveAction("OK") { dialog, _ ->
          dialog.dismiss()
          if (!emitter.isDisposed) {
            emitter.onNext(chapter)
            emitter.onComplete()
          }
        }
        onCancel {
          if (!emitter.isDisposed) {
            emitter.onComplete()
          }
        }
      }
      emitter.setCancellable { alertDialog.dismiss() }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
    Timber.d("DownloadingChaptersFragment::onDestroyView")
  }
}