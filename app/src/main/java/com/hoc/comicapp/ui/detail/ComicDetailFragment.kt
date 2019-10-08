package com.hoc.comicapp.ui.detail

import android.net.Uri
import android.os.Bundle
import android.util.Patterns
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
import com.hoc.comicapp.ui.detail.ComicDetailIntent.*
import com.hoc.comicapp.ui.detail.ComicDetailViewState.Chapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.*
import com.hoc.comicapp.utils.*
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import kotlinx.android.synthetic.main.fragment_comic_detail.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.io.File
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.absoluteValue
import com.hoc.comicapp.ui.detail.ComicDetailFragmentDirections.Companion.actionComicDetailFragmentToChapterDetailFragment as toChapterDetail

@ExperimentalCoroutinesApi
class ComicDetailFragment : Fragment() {
  private val viewModel by viewModel<ComicDetailViewModel>() { parametersOf(args.isDownloaded) }
  private val args by navArgs<ComicDetailFragmentArgs>()

  private val compositeDisposable = CompositeDisposable()
  private val glide by lazy(NONE) { GlideApp.with(this) }

  private val intentS = PublishRelay.create<ComicDetailIntent>()

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
      ::onClickButtonRead,
      ::onClickChapter
    )
    initView(chapterAdapter)
    bind(chapterAdapter)
  }

  private fun onClickDownload(chapter: Chapter) {
    //TODO: cancel worker???
    when (chapter.downloadState) {
      Downloaded -> {
        requireActivity().showAlertDialog {
          title("Delete from downloads")
          message("This chapter won't be available to read offline")
          cancelable(true)
          iconId(R.drawable.ic_delete_white_24dp)

          negativeAction("Cancel") { dialog, _ -> dialog.cancel() }
          positiveAction("OK") { dialog, _ ->
            intentS.accept(DeleteChapter(chapter))
            dialog.dismiss()
          }
        }
      }
      NotYetDownload -> {
        requireActivity().showAlertDialog {
          title("Download ${chapter.chapterName}")
          message("This chapter will download as soon as internet is connected")
          cancelable(true)
          iconId(R.drawable.ic_file_download_white_24dp)

          negativeAction("Cancel") { dialog, _ -> dialog.cancel() }
          positiveAction("OK") { dialog, _ ->
            intentS.accept(DownloadChapter(chapter))
            dialog.dismiss()
          }
        }
      }
      is Downloading -> {
        requireActivity().showAlertDialog {
          title("Cancel downloading")
          message("This chapter won't be available to read offline")
          cancelable(true)
          iconId(R.drawable.ic_delete_white_24dp)

          negativeAction("Cancel") { dialog, _ -> dialog.cancel() }
          positiveAction("OK") { dialog, _ ->
            intentS.accept(CancelDownloadChapter(chapter))
            dialog.dismiss()
          }
        }
      }
    }
  }

  private fun onClickButtonRead(readFirst: @ParameterName(name = "readFirst") Boolean) {
    val comicDetail = viewModel.state.value.comicDetail as? ComicDetail.Detail ?: return
    val chapter =
      comicDetail.chapters.let { if (readFirst) it.lastOrNull() else it.firstOrNull() }
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
        is ComicDetailSingleEvent.EnqueuedDownloadSuccess -> {
          view?.snack("Enqueued download ${it.chapter.chapterName}") {
            action("View") {
              findNavController().navigate(R.id.downloadingChaptersFragment)
            }
          }
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
//        TODO: Refresh detail page
//        swipe_refresh_layout
//          .refreshes()
//          .map { ComicDetailIntent.Refresh(argComic.link) }
        intentS
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
      is ComicDetail.Detail -> {
        text_title.text = detail.title

        val list = mutableListOf(
          "Last updated" to detail.lastUpdated,
          "View" to detail.view
        )
        text_last_updated_status_view.text = HtmlCompat.fromHtml(
          list.joinToString("<br>") { "\u2022 <b>${it.first}:</b> ${it.second}" },
          HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        loadThumbnail(detail.thumbnail)

        chapterAdapter.submitList(listOf(
          ChapterAdapterItem.Header(
            categories = detail.categories,
            shortenedContent = detail.shortenedContent
          )
        ) + detail.chapters.map { ChapterAdapterItem.Chapter(it) })
      }
      is ComicDetail.Initial -> {
        text_title.text = detail.title
        loadThumbnail(detail.thumbnail)
      }
    }
  }

  private fun loadThumbnail(thumbnail: String) {
    glide
      .load(
        when {
          Patterns.WEB_URL.matcher(thumbnail).matches() -> {
            Timber.d("load_thumbnail [1] $thumbnail")
            Uri.parse(thumbnail)
          }
          else -> {
            Timber.d("load_thumbnail [2] $thumbnail")
            Uri.fromFile(File(requireContext().filesDir, thumbnail))
          }
        }
      )
      .fitCenter()
      .transition(DrawableTransitionOptions.withCrossFade())
      .into(image_thumbnail)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    Timber.d("ComicDetailFragment::onDestroyView")

    compositeDisposable.clear()
    root_detail.setTransitionListener(null)
  }

  private fun onClickChapter(chapter: Chapter, view: View) {
    when (view.id) {
      R.id.image_download -> onClickDownload(chapter)
      else -> findNavController().navigate(toChapterDetail(chapter))
    }
  }
}