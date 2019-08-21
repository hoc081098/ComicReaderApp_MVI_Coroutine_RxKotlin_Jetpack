package com.hoc.comicapp.ui.chapter_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.MainActivity
import com.hoc.comicapp.domain.models.ChapterDetail
import com.hoc.comicapp.utils.itemSelections
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_chapter_detail.*
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

  private val mainActivity get() = requireActivity() as MainActivity

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_chapter_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val chapterImageAdapter = ChapterImageAdapter(GlideApp.with(this))
    initView(chapterImageAdapter)
    bind(chapterImageAdapter)
  }

  private fun initView(chapterImageAdapter: ChapterImageAdapter) {
    view_pager.adapter = chapterImageAdapter

    spinner_chapters.setItems(
      ChapterDetail.Chapter(
        chapterLink = navArgs.chapter.chapterLink,
        chapterName = navArgs.chapter.chapterName
      )
    )
    spinner_chapters.selectedIndex = 0
  }

  private fun bind(adapter: ChapterImageAdapter) {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is ChapterDetailSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, isRefreshing, errorMessage, detail, @ViewPager2.Orientation orientation) ->
      view_pager.orientation = orientation

      progress_bar.isVisible = isLoading

      group_error.isVisible = errorMessage !== null
      text_error_message.text = errorMessage
      when (detail) {
        is ChapterDetailViewState.Detail.Initial -> {
          mainActivity.setToolbarTitle(detail.chapterName)

          spinner_chapters.setItems(
            ChapterDetail.Chapter(
              chapterLink = detail.chapterLink,
              chapterName = detail.chapterName
            )
          )
          spinner_chapters.selectedIndex = 0
        }
        is ChapterDetailViewState.Detail.Data -> {
          val chapterDetail = detail.chapterDetail

          mainActivity.setToolbarTitle(chapterDetail.chapterName)

          adapter.submitList(if (errorMessage !== null) chapterDetail.images else emptyList())

          spinner_chapters.setItems(chapterDetail.chapters)
          spinner_chapters.selectedIndex =
            chapterDetail.chapters.indexOfFirst { it.chapterLink == chapterDetail.chapterLink }

          button_prev.isEnabled = chapterDetail.prevChapterLink !== null
          button_next.isEnabled = chapterDetail.nextChapterLink !== null
        }
      }
    }

    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(initial),
        button_next
          .clicks()
          .map { ChapterDetailViewIntent.LoadNextChapter },
        button_prev
          .clicks()
          .map { ChapterDetailViewIntent.LoadPrevChapter },
        button_retry
          .clicks()
          .map { ChapterDetailViewIntent.Retry },
        spinner_chapters
          .itemSelections<ChapterDetail.Chapter>()
          .skipInitialValue()
          .map { ChapterDetailViewIntent.LoadChapter(it.chapterLink, it.chapterName) }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}