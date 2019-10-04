package com.hoc.comicapp.ui.downloaded_comics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.*
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
import com.hoc.comicapp.utils.*
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_downloaded_comics.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class DownloadedComicsFragment : Fragment() {
  private val viewModel by viewModel<DownloadedComicsViewModel>()
  private val compositeDisposable = CompositeDisposable()

  private val viewBinderHelper = ViewBinderHelper()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_downloaded_comics, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val adapter = DownloadedComicsAdapter(
      GlideApp.with(this),
      viewBinderHelper,
      compositeDisposable
    )
    initView(adapter)
    bind(adapter)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewBinderHelper.restoreStates(savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    viewBinderHelper.saveStates(outState)
  }

  private fun initView(downloadedComicsAdapter: DownloadedComicsAdapter) {
    recycler_comics.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = downloadedComicsAdapter
    }

    spinner_sort.setItems(SortOrder.values().toList())
    spinner_sort.selectedIndex = viewModel.state.safeValue?.sortOrder?.let { SortOrder.values().indexOf(it) } ?: 0
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
        is SingleEvent.DeletedComic -> {
          view?.snack("Deleted ${it.comic.title}")
        }
        is SingleEvent.DeleteComicError -> {
          view?.snack("Error when deleting ${it.comic.title}, error: ${it.error.getMessage()}")
        }
      }
    }
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(ViewIntent.Initial),
        spinner_sort
          .itemSelections<SortOrder>()
          .map { ViewIntent.ChangeSortOrder(it) }
          .doOnNext { Timber.d("Sort $it") },
        adapter
          .clickDelete
          .doOnNext { Timber.d("Delete[1] $it") }
          .exhaustMap(::showDeleteComicDialog)
          .doOnNext { Timber.d("Delete[2] $it") }
          .map { ViewIntent.DeleteComic(it) }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }

  private fun showDeleteComicDialog(comic: ComicItem): Observable<ComicItem> {
    return Observable.create<ComicItem> { emitter ->
      val alertDialog = requireActivity().showAlertDialog {
        title("Delete comic")
        message("All chapter in this comic won't be available to read offline")
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
            emitter.onNext(comic)
            emitter.onComplete()
          }
        }
      }
      emitter.setCancellable { alertDialog.dismiss() }
    }
  }

}