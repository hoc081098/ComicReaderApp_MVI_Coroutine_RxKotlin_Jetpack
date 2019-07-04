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
import com.hoc.comicapp.utils.toast
import io.reactivex.Observable
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChapterDetailFragment : Fragment() {
  private val navArgs by navArgs<ChapterDetailFragmentArgs>()
  private val viewModel by viewModel<ChapterDetailViewModel>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_chapter_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bind()
  }

  private fun bind() {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is ChapterDetailSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.state.observe(owner = viewLifecycleOwner) {
      context?.toast(it.toString())
    }

    val chapter = navArgs.chapter
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(
          ChapterDetailViewIntent.Initial(
            initial = ChapterDetailViewState.Detail.Initial(
              chapterName = chapter.chapterName,
              time = chapter.time,
              view = chapter.view,
              chapterLink = chapter.chapterLink
            )
          )
        )
      )
    )
  }

  override fun onDestroyView() {
    super.onDestroyView()
  }
}