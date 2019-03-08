package com.hoc.comicapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.ComicDetailFragmentArgs
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel

@ExperimentalCoroutinesApi
class ComicDetailFragment : Fragment() {
  private val viewModel by viewModel<ComicDetailViewModel>()
  private val args by navArgs<ComicDetailFragmentArgs>()

  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_comic_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onResume() {
    super.onResume()

    val comic = args.comic
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(
          ComicDetailIntent.Initial(
            link = comic.link,
            thumbnail = comic.thumbnail,
            name = comic.title
          )
        )
      )
    ).addTo(compositeDisposable)
  }
}