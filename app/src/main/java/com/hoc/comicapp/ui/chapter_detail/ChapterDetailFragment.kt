package com.hoc.comicapp.ui.chapter_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.LazyThreadSafetyMode.NONE

class ChapterDetailFragment : Fragment() {
  private val navArgs by navArgs<ChapterDetailFragmentArgs>()
  private val viewModel by viewModel<ChapterDetailViewModel>()
  private val initial by lazy(NONE) {
    val chapter = navArgs.chapter
    ChapterDetailViewIntent.Initial(
      initial = ChapterDetailViewState.Detail.Initial(
        chapterName = chapter.chapterName,
        time = chapter.time,
        view = chapter.view,
        chapterLink = chapter.chapterLink
      )
    )
  }

  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_chapter_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initView()
    bind()
  }

  private fun initView() {

  }

  private fun bind() {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is ChapterDetailSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, isRefreshing, errorMessage, detail) ->
      if (isLoading) {

      } else {

      }
      if (isRefreshing) {

      } else {

      }
      if (errorMessage !== null) {

      } else {

      }
      when (detail ?: return@observe) {
        is ChapterDetailViewState.Detail.Initial -> {

        }
        is ChapterDetailViewState.Detail.Data -> {

        }
      }
    }

    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(initial)
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}