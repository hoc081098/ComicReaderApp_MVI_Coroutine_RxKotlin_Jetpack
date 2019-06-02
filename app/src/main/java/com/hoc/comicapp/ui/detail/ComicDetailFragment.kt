package com.hoc.comicapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.toast
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
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
    recycler_chapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chapterAdapter
    }
  }

  private fun bind(chapterAdapter: ChapterAdapter) {
    viewModel.state.observe(viewLifecycleOwner) { render(it, chapterAdapter) }

    viewModel.singleEvent.observeEvent(viewLifecycleOwner) {
      when (it) {
        is ComicDetailSingleEvent.MessageEvent -> {
          view?.snack(it.message)
        }
      }
    }

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

  private fun render(
    viewState: ComicDetailViewState,
    chapterAdapter: ChapterAdapter
  ) {
    Timber.d("state=$viewState")
    val comicDetail = viewState.comicDetail ?: return

    text_title.text = comicDetail.title
    text_last_updated.text = "Loading..."
    glide
      .load(comicDetail.thumbnail)
      .fitCenter()
      .transition(DrawableTransitionOptions.withCrossFade())
      .into(image_thumbnail)

    if (comicDetail is ComicDetail.Comic) {
      text_last_updated.text = "Last updated: ${comicDetail.lastUpdated}"
      chapterAdapter.submitList(comicDetail.chapters)
      progress_bar.visibility = View.INVISIBLE
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