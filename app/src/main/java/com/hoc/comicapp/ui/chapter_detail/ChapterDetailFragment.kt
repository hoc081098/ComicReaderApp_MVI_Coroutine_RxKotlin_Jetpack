package com.hoc.comicapp.ui.chapter_detail

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.ViewPager2
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.main.MainActivity
import com.hoc.comicapp.base.BaseFragment
import com.hoc.comicapp.databinding.FragmentChapterDetailBinding
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.SingleEvent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewIntent
import com.hoc.comicapp.ui.chapter_detail.ChapterDetailContract.ViewState
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.unit
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.widget.checkedChanges
import io.reactivex.rxjava3.core.Observable
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class ChapterDetailFragment : BaseFragment<
  ViewIntent,
  ViewState,
  SingleEvent,
  ChapterDetailViewModel
  >(R.layout.fragment_chapter_detail) {
  private val navArgs by navArgs<ChapterDetailFragmentArgs>()
  override val viewModel by viewModel<ChapterDetailViewModel> {
    parametersOf(navArgs.isDownloaded)
  }
  override val viewBinding by viewBinding<FragmentChapterDetailBinding> {
    recyclerImages.adapter = null
    spinnerChapters.adapter = null
  }

  private val mainActivity get() = requireActivity() as MainActivity
  private var shouldEmitSelectedItem = false

  private val chapterImageAdapter by lazy(NONE) { ChapterImageAdapter(GlideApp.with(this)) }
  private val allChaptersAdapter by lazy(NONE) {
    ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      mutableListOf(
        ViewState.Chapter(
          link = navArgs.chapter.chapterLink,
          name = navArgs.chapter.chapterName
        )
      )
    )
  }

  //region Override BaseFragment
  override fun setupView(view: View, savedInstanceState: Bundle?) = viewBinding.run {
    recyclerImages.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      adapter = chapterImageAdapter
    }

    spinnerChapters.adapter = allChaptersAdapter

    viewModel.state.value.detail?.let { detail ->
      allChaptersAdapter.clear()
      allChaptersAdapter.addAll(detail.chapters)

      val index = detail.chapters.indexOfFirst { it == detail.chapter }
      spinnerChapters.setSelection(index, false)

      Timber.tag("LoadChapter###").d("::initView $index $detail")
    }

    Unit
  }

  override fun render(viewState: ViewState) = viewBinding.run {
    val (isLoading, isRefreshing, errorMessage, detail, @ViewPager2.Orientation orientation) = viewState
    Timber.d("chapter_detail_state=[$isLoading, $isRefreshing, $errorMessage, $detail, $orientation]")

    shouldEmitSelectedItem = false

    (recyclerImages.layoutManager as LinearLayoutManager).orientation = orientation

    progressBar.isVisible = isLoading

    groupError.isVisible = errorMessage !== null
    textErrorMessage.text = errorMessage

    detail ?: return
    mainActivity.setToolbarTitle(detail.chapter.name)

    allChaptersAdapter.clear()
    allChaptersAdapter.addAll(detail.chapters)

    val index = detail.chapters.indexOfFirst { it == detail.chapter }
    spinnerChapters.setSelection(index, false)
    Timber.tag("LoadChapter###").d("Index=$index, chapter=${detail.chapter.debug}")

    when (detail) {
      is ViewState.Detail.Data -> {
        chapterImageAdapter.submitList(detail.images)

        TransitionManager.beginDelayedTransition(bottomNav, AutoTransition())
        buttonPrev.isInvisible = detail.prevChapterLink === null
        buttonNext.isInvisible = detail.nextChapterLink === null
      }
      is ViewState.Detail.Initial -> {
        chapterImageAdapter.submitList(emptyList())

        TransitionManager.beginDelayedTransition(bottomNav, AutoTransition())
        buttonPrev.isInvisible = true
        buttonNext.isInvisible = true
      }
    }

    shouldEmitSelectedItem = true
  }

  override fun handleEvent(event: SingleEvent) {
    return when (event) {
      is SingleEvent.MessageEvent -> {
        view?.snack(event.message)
      }
    }.unit
  }

  override fun viewIntents(): Observable<ViewIntent> = viewBinding.run {
    val chapterItemSelections = Observable.create<ViewState.Chapter> { emitter ->
      spinnerChapters.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {}

        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
          if (shouldEmitSelectedItem && !emitter.isDisposed) {
            emitter.onNext(parent.getItemAtPosition(position) as ViewState.Chapter)
          }
        }
      }

      emitter.setCancellable { spinnerChapters.onItemSelectedListener = null }
    }

    Observable.mergeArray(
      Observable.just(
        ViewIntent.Initial(
          ViewState.Chapter(
            name = navArgs.chapter.chapterName,
            link = navArgs.chapter.chapterLink
          )
        )
      ),
      buttonNext
        .clicks()
        .map { ViewIntent.LoadNextChapter },
      buttonPrev
        .clicks()
        .map { ViewIntent.LoadPrevChapter },
      buttonRetry
        .clicks()
        .map { ViewIntent.Retry },
      chapterItemSelections
        .map { ViewIntent.LoadChapter(it) },
      switchOrientation
        .checkedChanges()
        .map { ViewIntent.ChangeOrientation(if (it) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL) }
    ).doOnNext { Timber.tag("LoadChapter###").d("Intent $it") }
  }
  //endregion
}
