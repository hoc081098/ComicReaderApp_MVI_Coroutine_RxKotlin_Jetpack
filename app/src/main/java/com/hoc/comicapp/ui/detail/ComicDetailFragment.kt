package com.hoc.comicapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.ComicDetail.Chapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.snack
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import kotlinx.android.synthetic.main.fragment_comic_detail.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.absoluteValue
import com.hoc.comicapp.ui.detail.ComicDetailFragmentDirections.Companion.actionComicDetailFragmentToChapterDetailFragment as toChapterDetail

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

    val chapterAdapter = ChapterAdapter(
      ::onClickChapter,
      ::onClickButtonRead,
      ::onClickDownload
    )
    initView(chapterAdapter)
    bind(chapterAdapter)
  }

  private fun onClickDownload(chapter: Chapter) {
//TODO
  }


  private fun onClickButtonRead(readFirst: @ParameterName(name = "readFirst") Boolean) {
    val comicDetail = viewModel.state.value.comicDetail as? ComicDetail.Comic ?: return
    val chapter =
      comicDetail.comicDetail.chapters.let { if (readFirst) it.lastOrNull() else it.firstOrNull() }
    if (chapter === null) {
      view?.snack("Chapters list is empty!")
    } else {
      findNavController().navigate(toChapterDetail(chapter))
    }
  }

  private fun initView(chapterAdapter: ChapterAdapter) {
//    TODO: Refresh detail page
//    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(com.hoc.comicapp.R.array.swipe_refresh_colors))

    recycler_chapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chapterAdapter
    }

    setupFab(chapterAdapter)
    setupMotionLayout()
  }

  private fun setupMotionLayout() {
    root_detail
      .getConstraintSet(R.layout.fragment_comic_detail)
      .setGuidelinePercent(
        R.id.guideline,
        if (requireContext().isOrientationPortrait) 0.45f else 0.175f
      )

    root_detail
      .getConstraintSet(R.layout.fragment_comic_detail_end)
      .setGuidelinePercent(
        R.id.guideline,
        if (requireContext().isOrientationPortrait) 0.175f else 0.05f
      )


    var lastProgress = 0f
    root_detail.setTransitionListener(object : TransitionAdapter() {
      override fun onTransitionChange(
        motionLayout: MotionLayout?,
        startId: Int,
        endId: Int,
        progress: Float
      ) {
        if (progress - lastProgress > 0) {
          // from start to end
          if ((progress - 1f).absoluteValue < 0.5f) {
            text_title.maxLines = 1
            text_last_updated_status_view.maxLines = 2
            Timber.d("END")
          }
        } else {
          // from end to start
          if (progress < 0.3f) {
            text_title.maxLines = 6
            text_last_updated_status_view.maxLines = Int.MAX_VALUE
            Timber.d("START")
          }
        }
        lastProgress = progress
      }
    })
  }

  private fun setupFab(chapterAdapter: ChapterAdapter) {
    val scrollEvents = recycler_chapters.scrollEvents().share()
    scrollEvents
      .subscribeBy {
        when {
          it.dy > 0 -> {
            fab.show()
            fab.setImageResource(R.drawable.ic_arrow_downward_white_24dp)
          }
          it.dy < 0 -> {
            fab.show()
            fab.setImageResource(R.drawable.ic_arrow_upward_white_24dp)
          }
          else -> fab.hide()
        }
      }
      .addTo(compositeDisposable)

    val smoothScroller = object : LinearSmoothScroller(requireContext()) {
      override fun getVerticalSnapPreference() = SNAP_TO_START
    }
    fab
      .clicks()
      .withLatestFrom(scrollEvents)
      .subscribeBy {
        smoothScroller
          .apply {
            val dy = it.second.dy
            targetPosition = when {
              dy == 0 -> return@subscribeBy
              dy > 0 -> chapterAdapter.itemCount - 1
              else -> 0
            }
          }
          .let { recycler_chapters.layoutManager!!.startSmoothScroll(it) }
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
          .map { ComicDetailIntent.Retry(argComic.link) }
//        TODO: Refresh detail page
//        swipe_refresh_layout
//          .refreshes()
//          .map { ComicDetailIntent.Refresh(argComic.link) }
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
      text_last_updated_status_view.text = "Loading..."
    } else {
      progress_bar.visibility = View.INVISIBLE
    }

    if (viewState.errorMessage === null) {
      group_error.visibility = View.GONE
    } else {
      group_error.visibility = View.VISIBLE
      text_error_message.text = viewState.errorMessage
      text_last_updated_status_view.text = "Error occurred"
    }

//    TODO: Refresh detail page
//    if (!viewState.isRefreshing) {
//      swipe_refresh_layout.isRefreshing = false
//    }

    when (val detail = viewState.comicDetail ?: return) {
      is ComicDetail.Comic -> {
        // actual comic detail state
        val comicDetail = detail.comicDetail

        text_title.text = comicDetail.title

        val list = mutableListOf(
          "Last updated" to comicDetail.lastUpdated,
          "View" to comicDetail.view
        )
        text_last_updated_status_view.text = HtmlCompat.fromHtml(
          list.joinToString("<br>") { "\u2022 <b>${it.first}:</b> ${it.second}" },
          HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        glide
          .load(comicDetail.thumbnail)
          .fitCenter()
          .transition(DrawableTransitionOptions.withCrossFade())
          .into(image_thumbnail)

        chapterAdapter.submitList(listOf(
          ChapterItem.Header(
            categories = comicDetail.categories,
            shortenedContent = comicDetail.shortenedContent
          )
        ) + comicDetail.chapters.map { ChapterItem.Chapter(it) })
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
    root_detail.setTransitionListener(null)
  }

  private fun onClickChapter(chapter: Chapter) =
    findNavController().navigate(toChapterDetail(chapter))
}