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
import com.hoc.comicapp.utils.toast
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_comic_detail.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

@ExperimentalCoroutinesApi
class ComicDetailFragment : Fragment() {
  private val viewModel by viewModel<ComicDetailViewModel>()
  private val args by navArgs<ComicDetailFragmentArgs>()

  private val compositeDisposable = CompositeDisposable()
  private val chapterAdapter = ChapterAdapter(::onClickChapter)

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_comic_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initView()
    subscribeVM()
  }

  private fun initView() {
    recycler_chapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chapterAdapter
    }
  }

  private fun subscribeVM() {
    viewModel.state.observe(this) {
      Timber.d("state = $it")

      val comicDetail = it.comicDetail ?: return@observe

      text_title.text = comicDetail.title
      GlideApp
        .with(image_thumbnail.context)
        .load(comicDetail.thumbnail)
        .thumbnail(0.5f)
        .fitCenter()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(image_thumbnail)

      if (comicDetail is ComicDetail.Comic) {
        chapterAdapter.submitList(comicDetail.chapters)
        progress_bar.visibility = View.INVISIBLE
      }
    }
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

  override fun onPause() {
    super.onPause()
    compositeDisposable.clear()
  }

  private fun onClickChapter(chapter: Chapter) {
    requireContext().toast("Clicked $chapter")
  }
}