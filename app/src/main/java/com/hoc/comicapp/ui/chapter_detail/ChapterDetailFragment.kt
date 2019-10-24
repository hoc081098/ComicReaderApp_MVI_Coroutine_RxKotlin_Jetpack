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
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailViewIntent.*
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
  private val viewModel by viewModel<ChapterDetailViewModel>() { parametersOf(navArgs.isDownloaded) }

  private val compositeDisposable = CompositeDisposable()

  private val mainActivity get() = requireActivity() as MainActivity
  private var shouldEmitSelectedItem = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_chapter_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val chapterImageAdapter = ChapterImageAdapter(GlideApp.with(this))
    val allChaptersAdapter = ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      mutableListOf(
        ChapterDetailViewState.Chapter(
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
    allChaptersAdapter: ArrayAdapter<ChapterDetailViewState.Chapter>
  ) {
    recycler_images.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      adapter = chapterImageAdapter
    }


    spinner_chapters.adapter = allChaptersAdapter
    spinner_chapters.setSelection(0, false)
  }

  private fun bind(
    imageAdapter: ChapterImageAdapter,
    allChaptersAdapter: ArrayAdapter<ChapterDetailViewState.Chapter>
  ) {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is ChapterDetailSingleEvent.MessageEvent -> {
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

      when (detail) {
        is ChapterDetailViewState.Detail.Data -> {
          imageAdapter.submitList(detail.images)

          allChaptersAdapter.clear()
          allChaptersAdapter.addAll(detail.chapters)

          val index = detail.chapters.indexOfFirst { it == detail.chapter }
          spinner_chapters.setSelection(index, false)
          Timber.tag("LoadChapter###").d("Index=$index, chapter=${detail.chapter.debug}")


          TransitionManager.beginDelayedTransition(bottom_nav, AutoTransition())
          button_prev.isInvisible = detail.prevChapterLink === null
          button_next.isInvisible = detail.nextChapterLink === null
        }
        is ChapterDetailViewState.Detail.Initial -> {
          imageAdapter.submitList(emptyList())
        }
      }

      shouldEmitSelectedItem = true
    }

    val chapterItemSelections = Observable.create<ChapterDetailViewState.Chapter> { emitter ->
      spinner_chapters.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
          if (shouldEmitSelectedItem && !emitter.isDisposed) {
            emitter.onNext(parent.getItemAtPosition(position) as ChapterDetailViewState.Chapter)
          }
        }
      }

      emitter.setCancellable { spinner_chapters.onItemSelectedListener = null }
    }
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(
          Initial(
            ChapterDetailViewState.Chapter(
              name = navArgs.chapter.chapterName,
              link = navArgs.chapter.chapterLink
            )
          )
        ),
        button_next
          .clicks()
          .map { LoadNextChapter },
        button_prev
          .clicks()
          .map { LoadPrevChapter },
        button_retry
          .clicks()
          .map { Retry },
        chapterItemSelections
          .map { LoadChapter(it) }
          .doOnNext { Timber.tag("LoadChapter###").d("Load intent ${it.chapter.debug}") },
        switch_orientation
          .checkedChanges()
          .map { ChangeOrientation(if (it) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL) }
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}