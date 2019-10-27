package com.hoc.comicapp.ui.favorite_comics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.chauthai.swipereveallayout.ViewBinderHelper
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.ui.detail.ComicArg
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.SortOrder
import com.hoc.comicapp.ui.favorite_comics.FavoriteComicsContract.ViewIntent
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.itemSelections
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.showAlertDialogAsObservable
import com.hoc.comicapp.utils.snack
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_favorite_comics.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class FavoriteComicsFragment : Fragment() {
  private val compositeDisposable = CompositeDisposable()
  private val viewModel by viewModel<FavoriteComicsVM>()

  private val viewBinderHelper = ViewBinderHelper()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(R.layout.fragment_favorite_comics, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val adapter = FavoriteComicsAdapter(
      GlideApp.with(this),
      viewBinderHelper,
      compositeDisposable
    )
    initView(adapter)
    bindVM(adapter)
  }

  private fun initView(favoriteComicsAdapter: FavoriteComicsAdapter) {
    recycler_comics.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = favoriteComicsAdapter
    }

    spinner_sort.setItems(SortOrder.values().toList())
    spinner_sort.selectedIndex = viewModel.state.safeValue?.sortOrder
      ?.let { SortOrder.values().indexOf(it) } ?: 0

    favoriteComicsAdapter
      .clickItem
      .map {
        ComicArg(
          link = it.url,
          thumbnail = it.thumbnail,
          title = it.title
        )
      }
      .map {
        FavoriteComicsFragmentDirections.actionFavoriteComicsFragmentToComicDetailFragment(
          comic = it,
          isDownloaded = false,
          title = it.title
        )
      }
      .subscribeBy(onNext = findNavController()::navigate)
      .addTo(compositeDisposable)
  }


  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewBinderHelper.restoreStates(savedInstanceState)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    viewBinderHelper.saveStates(outState)
  }

  private fun bindVM(adapter: FavoriteComicsAdapter) {
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, error, comics) ->
      if (isLoading) {
        progress_bar.visibility = View.VISIBLE
      } else {
        progress_bar.visibility = View.INVISIBLE
      }

      if (error == null) {
        group_error.visibility = View.GONE
      } else {
        group_error.visibility = View.VISIBLE
        text_error_message.text = error.getMessage()
      }

      adapter.submitList(comics)

      empty_layout.isVisible = !isLoading && error === null && comics.isEmpty()
    }

    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) { event ->
      when (event) {
        is FavoriteComicsContract.SingleEvent.Message -> {
          view?.snack(event.message)
        }
      }
    }

    viewModel
      .processIntents(
        Observable.mergeArray(
          Observable.just(ViewIntent.Initial),
          adapter.clickDelete
            .exhaustMap { item ->
              requireActivity()
                .showAlertDialogAsObservable {
                  title("Remove favorite")
                  message("Remove this comic from favorites")
                  cancelable(true)
                  iconId(R.drawable.ic_delete_white_24dp)
                }
                .map { item }
            }
            .map { ViewIntent.Remove(it) },
          spinner_sort
            .itemSelections<SortOrder>()
            .map { ViewIntent.ChangeSortOrder(it) }
        )
      )
      .addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}