package com.hoc.comicapp.ui.chapter_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.main.MainActivity
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.SingleEvent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewIntent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.widget.checkedChanges
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_chapter_detail.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ChapterDetailFragment : Fragment() {
  private val navArgs by navArgs<ChapterDetailFragmentArgs>()
  private val viewModel by viewModel<ChapterDetailViewModel> { parametersOf(navArgs.isDownloaded) }

  private val compositeDisposable = CompositeDisposable()

  private val mainActivity get() = requireActivity() as MainActivity
  private var shouldEmitSelectedItem = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View = inflater.inflate(R.layout.fragment_chapter_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val chapterImageAdapter = ChapterImageAdapter(GlideApp.with(this))
    val allChaptersAdapter = ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      mutableListOf(
        ViewState.Chapter(
          link = navArgs.chapter.chapterLink,
          name = navArgs.chapter.chapterName
        )
      )
    )

    initView(chapterImageAdapter, allChaptersAdapter)
    bind(chapterImageAdapter, allChaptersAdapter)
  }

  private fun initView(
    chapterImageAdapter: ChapterImageAdapter,
    allChaptersAdapter: ArrayAdapter<ViewState.Chapter>,
  ) {
    recycler_images.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      adapter = chapterImageAdapter
    }


    spinner_chapters.adapter = allChaptersAdapter

    viewModel.state.safeValue?.detail?.let { detail ->
      allChaptersAdapter.clear()
      allChaptersAdapter.addAll(detail.chapters)

      val index = detail.chapters.indexOfFirst { it == detail.chapter }
      spinner_chapters.setSelection(index, false)

      Timber.tag("LoadChapter###").d("::initView $index $detail")
    }
  }

  private fun bind(
    imageAdapter: ChapterImageAdapter,
    allChaptersAdapter: ArrayAdapter<ViewState.Chapter>,
  ) {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is SingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }
    viewModel.state.observe(owner = viewLifecycleOwner) { (isLoading, isRefreshing, errorMessage, detail, @ViewPager2.Orientation orientation) ->
      Timber.d("chapter_detail_state=[$isLoading, $isRefreshing, $errorMessage, $detail, $orientation]")

      shouldEmitSelectedItem = false

      (recycler_images.layoutManager as LinearLayoutManager).orientation = orientation

      progress_bar.isVisible = isLoading

      group_error.isVisible = errorMessage !== null
      text_error_message.text = errorMessage

      detail ?: return@observe
      mainActivity.setToolbarTitle(detail.chapter.name)

      allChaptersAdapter.clear()
      allChaptersAdapter.addAll(detail.chapters)

      val index = detail.chapters.indexOfFirst { it == detail.chapter }
      spinner_chapters.setSelection(index, false)
      Timber.tag("LoadChapter###").d("Index=$index, chapter=${detail.chapter.debug}")

      when (detail) {
        is ViewState.Detail.Data -> {
          imageAdapter.submitList(detail.images)

          TransitionManager.beginDelayedTransition(bottom_nav, AutoTransition())
          button_prev.isInvisible = detail.prevChapterLink === null
          button_next.isInvisible = detail.nextChapterLink === null
        }
        is ViewState.Detail.Initial -> {
          imageAdapter.submitList(emptyList())

          TransitionManager.beginDelayedTransition(bottom_nav, AutoTransition())
          button_prev.isInvisible = true
          button_next.isInvisible = true
        }
      }

      shouldEmitSelectedItem = true
    }

    val chapterItemSelections = Observable.create<ViewState.Chapter> { emitter ->
      spinner_chapters.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
          if (shouldEmitSelectedItem && !emitter.isDisposed) {
            emitter.onNext(parent.getItemAtPosition(position) as ViewState.Chapter)
          }
        }
      }

      emitter.setCancellable { spinner_chapters.onItemSelectedListener = null }
    }
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(
          ViewIntent.Initial(
            ViewState.Chapter(
              name = navArgs.chapter.chapterName,
              link = navArgs.chapter.chapterLink
            )
          )
        ),
        button_next
          .clicks()
          .map { ViewIntent.LoadNextChapter },
        button_prev
          .clicks()
          .map { ViewIntent.LoadPrevChapter },
        button_retry
          .clicks()
          .map { ViewIntent.Retry },
        chapterItemSelections
          .map { ViewIntent.LoadChapter(it) },
        switch_orientation
          .checkedChanges()
          .map { ViewIntent.ChangeOrientation(if (it) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL) }
      ).doOnNext { Timber.tag("LoadChapter###").d("Intent $it") }
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}