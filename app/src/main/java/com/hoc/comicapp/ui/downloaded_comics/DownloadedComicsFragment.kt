package com.hoc.comicapp.ui.downloaded_comics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.FragmentDownloadedComicsBinding
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.SingleEvent
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.SortOrder
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewIntent
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsContract.ViewState.ComicItem
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.itemSelections
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.showAlertDialogAsMaybe
import com.hoc.comicapp.utils.snack
import com.hoc081098.viewbindingdelegate.viewBinding
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import org.koin.androidx.scope.ScopeFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import com.hoc.comicapp.ui.downloaded_comics.DownloadedComicsFragmentDirections.Companion.actionDownloadedComicsFragmentToComicDetailFragment as toComicDetailFragment

class DownloadedComicsFragment : ScopeFragment() {
  private val viewModel by viewModel<DownloadedComicsViewModel>()
  private val viewBinding by viewBinding<FragmentDownloadedComicsBinding>()
  private val compositeDisposable = CompositeDisposable()

  private val viewBinderHelper = ViewBinderHelper()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View? {
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

    viewBinderHelper.restoreStates(savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    viewBinderHelper.saveStates(outState)
  }

  private fun initView(downloadedComicsAdapter: DownloadedComicsAdapter) = viewBinding.run {
    recyclerComics.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = downloadedComicsAdapter
    }

    spinnerSort.setItems(SortOrder.values().toList())
    spinnerSort.selectedIndex =
      viewModel.state.value.sortOrder.let { SortOrder.values().indexOf(it) }

    downloadedComicsAdapter.clickItem
      .subscribeBy { item ->
        requireAppNavigator.execute {
          navigate(
            toComicDetailFragment(
              comic = Arguments.ComicDetailArgs(
                title = item.title,
                thumbnail = item.thumbnail.toRelativeString(requireContext().filesDir),
                link = item.comicLink,
                view = item.view,
                remoteThumbnail = item.remoteThumbnail
              ),
              title = item.title,
              isDownloaded = true
            )
          )
        }
      }
      .addTo(compositeDisposable)
  }

  private fun bind(adapter: DownloadedComicsAdapter) = viewBinding.run {
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, errorMessage, comics) ->

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
        spinnerSort
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
    return requireActivity()
      .showAlertDialogAsMaybe {
        title("Delete comic")
        message("All chapter in this comic won't be available to read offline")
        cancelable(true)
        iconId(R.drawable.ic_delete_white_24dp)
      }
      .map { comic }
      .toObservable()
  }
}
