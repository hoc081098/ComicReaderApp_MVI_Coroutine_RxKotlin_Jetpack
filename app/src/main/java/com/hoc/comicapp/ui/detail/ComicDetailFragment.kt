package com.hoc.comicapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.ComicDetail.Chapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.toast
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_comic_detail.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

@ExperimentalCoroutinesApi
class ComicDetailFragment : Fragment() {
  private val viewModel by viewModel<ComicDetailViewModel>()
  private val args by navArgs<ComicDetailFragmentArgs>()

  private val compositeDisposable = CompositeDisposable()
  private val glide by lazy(NONE) { GlideApp.with(this) }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_comic_detail, container, false)
    .also { Timber.d("ComicDetailFragment::onCreateView") }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    Timber.d("ComicDetailFragment::onViewCreated")

    val chapterAdapter = ChapterAdapter(::onClickChapter)
    initView(chapterAdapter)
    bind(chapterAdapter)
  }

  private fun initView(chapterAdapter: ChapterAdapter) {
    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(com.hoc.comicapp.R.array.swipe_refresh_colors))

    recycler_chapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chapterAdapter
    }

    fab.setOnClickListener {
      object : LinearSmoothScroller(it.context) {
        override fun getVerticalSnapPreference() = LinearSmoothScroller.SNAP_TO_START
      }.apply { targetPosition = 0 }.let { recycler_chapters.layoutManager!!.startSmoothScroll(it) }
    }

    recycler_chapters
      .scrollEvents()
      .subscribeBy {
        if (it.dy < 0) {
          fab.show()
        } else {
          fab.hide()
        }
      }
      .addTo(compositeDisposable)
  }

  private fun bind(chapterAdapter: ChapterAdapter) {
    viewModel.state.observe(owner = viewLifecycleOwner) { render(it, chapterAdapter) }

    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {
      when (it) {
        is ComicDetailSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }

    val argComic = args.comic
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(
          ComicDetailIntent.Initial(
            link = argComic.link,
            title = argComic.title,
            thumbnail = argComic.thumbnail
          )
        ),
        button_retry
          .clicks()
          .map { ComicDetailIntent.Retry(argComic.link) },
        swipe_refresh_layout
          .refreshes()
          .map { ComicDetailIntent.Refresh(argComic.link) }
      )
    ).addTo(compositeDisposable)
  }

  private fun render(
    viewState: ComicDetailViewState,
    chapterAdapter: ChapterAdapter
  ) {
    Timber.d("state=$viewState")

    if (viewState.isLoading) {
      progress_bar.visibility = View.VISIBLE
      text_last_updated.text = "Loading..."
    } else {
      progress_bar.visibility = View.INVISIBLE
    }

    if (viewState.errorMessage === null) {
      group_error.visibility = View.GONE
    } else {
      group_error.visibility = View.VISIBLE
      text_error_message.text = viewState.errorMessage
      text_last_updated.text = "Error occurred"
    }

    if (!viewState.isRefreshing) {
      swipe_refresh_layout.isRefreshing = false
    }

    when (val detail = viewState.comicDetail) {
      null -> return
      is ComicDetail.Comic -> {
        // actual comic detail state
        val comicDetail = detail.comicDetail

        text_title.text = comicDetail.title
        text_last_updated.text = "Last updated: ${comicDetail.lastUpdated}"

        glide
          .load(comicDetail.thumbnail)
          .fitCenter()
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(image_thumbnail)

        chapterAdapter.submitList(comicDetail.chapters)
      }
      is ComicDetail.InitialComic -> {
        text_title.text = detail.title

        glide
          .load(detail.thumbnail)
          .fitCenter()
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(image_thumbnail)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("ComicDetailFragment::onDestroyView")
    compositeDisposable.clear()
  }

  private fun onClickChapter(chapter: Chapter) {
    requireContext().toast("Clicked $chapter")
  }
}