package com.hoc.comicapp.ui.detail

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.text.HtmlCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.transition.MaterialContainerTransform
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.base.BaseFragment
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract
import com.hoc.comicapp.ui.detail.ComicDetailIntent.CancelDownloadChapter
import com.hoc.comicapp.ui.detail.ComicDetailIntent.DeleteChapter
import com.hoc.comicapp.ui.detail.ComicDetailIntent.DownloadChapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.Chapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloaded
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloading
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.NotYetDownload
import com.hoc.comicapp.utils.action
import com.hoc.comicapp.utils.getDrawableBy
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.showAlertDialog
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.themeInterpolator
import com.jakewharton.rxbinding3.recyclerview.scrollEvents
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.withLatestFrom
import kotlinx.android.synthetic.main.fragment_comic_detail.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.scope.lifecycleScope
import org.koin.androidx.viewmodel.scope.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.absoluteValue
import com.hoc.comicapp.ui.detail.ComicDetailFragmentDirections.Companion.actionComicDetailFragmentToChapterDetailFragment as toChapterDetail

@ExperimentalCoroutinesApi
class ComicDetailFragment : BaseFragment<
    ComicDetailIntent,
    ComicDetailViewState,
    ComicDetailSingleEvent,
    ComicDetailViewModel
    >(R.layout.fragment_comic_detail) {
  override val viewModel by lifecycleScope.viewModel<ComicDetailViewModel>(owner = this) {
    parametersOf(args.isDownloaded)
  }
  private val args by navArgs<ComicDetailFragmentArgs>()

  private val glide by lazy(NONE) { GlideApp.with(this) }
  private val chapterAdapter by lazy(NONE) {
    ChapterAdapter(
      ::onClickButtonRead,
      ::onClickChapter,
      ::onClickChapterChip
    )
  }
  private val intentS = PublishRelay.create<ComicDetailIntent>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    prepareTransitions()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recycler_chapters.adapter = null
    root_detail.setTransitionListener(null)
  }

  //region Setup view
  private fun prepareTransitions() {
    postponeEnterTransition()

    sharedElementEnterTransition = MaterialContainerTransform(requireContext()).apply {
      // Scope the transition to a view in the hierarchy so we know it will be added under
      // the bottom app bar but over the Hold transition from the exiting HomeFragment.
      drawingViewId = R.id.main_nav_fragment
      duration = resources.getInteger(R.integer.reply_motion_default_large).toLong()
      interpolator = requireContext().themeInterpolator(R.attr.motionInterpolatorPersistent)
    }
    sharedElementReturnTransition = MaterialContainerTransform(requireContext()).apply {
      // Again, scope the return transition so it is added below the bottom app bar.
      drawingViewId = R.id.recycler_home
      duration = resources.getInteger(R.integer.reply_motion_default_large).toLong()
      interpolator = requireContext().themeInterpolator(R.attr.motionInterpolatorPersistent)
    }
  }

  private fun startTransitions() {
    Timber.d("transitionName: ${args.transitionName}")
    root_detail.transitionName = args.transitionName
    startPostponedEnterTransition()
  }

  private fun loadThumbnail(thumbnail: String) {
    val localFile = File(requireContext().filesDir, thumbnail)

    when {
      localFile.exists() -> {
        Timber.d("load_thumbnail [local] $thumbnail")
        Uri.fromFile(localFile)
      }
      else -> {
        Timber.d("load_thumbnail [remote] $thumbnail")
        Uri.parse(thumbnail)
      }
    }
      .let(glide::load)
      .fitCenter()
      .transition(DrawableTransitionOptions.withCrossFade())
      .into(image_thumbnail)
  }

  private fun setupFab() {
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
        progress: Float,
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
  //endregion

  //region Handle adapter click event
  private fun onClickChapter(chapter: Chapter, view: View) {
    when (view.id) {
      R.id.image_download -> onClickDownload(chapter)
      else -> findNavController().navigate(
        toChapterDetail(
          chapter = chapter,
          isDownloaded = args.isDownloaded
        )
      )
    }
  }

  private fun onClickChapterChip(category: ComicDetailViewState.Category) {
    val toCategoryDetailFragment =
      ComicDetailFragmentDirections.actionComicDetailFragmentToCategoryDetailFragment(
        title = category.name,
        category = CategoryDetailContract.CategoryArg(
          description = "",
          link = category.link,
          name = category.name,
          thumbnail = ""
        )
      )
    findNavController().navigate(toCategoryDetailFragment)
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
      findNavController().navigate(
        toChapterDetail(
          chapter = chapter,
          isDownloaded = args.isDownloaded
        )
      )
    }
  }
  //endregion

  //region Override BaseFragment
  override fun render(viewState: ComicDetailViewState) {
    Timber.d("state=$viewState")
    Timber.d("favorite=${viewState.isFavorited}")

    image_favorite.setImageDrawable(
      when (viewState.isFavorited) {
        true -> requireContext().getDrawableBy(id = R.drawable.ic_favorite_white_24dp)
        false -> requireContext().getDrawableBy(id = R.drawable.ic_favorite_border_white_24dp)
        null -> null
      }
    )

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
        ) + detail.chapters.map { ChapterAdapterItem.Chapter(it) } + ChapterAdapterItem.Dummy)
      }
      is ComicDetail.Initial -> {
        text_title.text = detail.title
        loadThumbnail(detail.thumbnail)
      }
    }
  }

  override fun handleEvent(event: ComicDetailSingleEvent) {
    when (event) {
      is ComicDetailSingleEvent.MessageEvent -> {
        view?.snack(event.message)
      }
      is ComicDetailSingleEvent.EnqueuedDownloadSuccess -> {
        view?.snack("Enqueued download ${event.chapter.chapterName}") {
          action("View") {
            findNavController().navigate(R.id.downloadingChaptersFragment)
          }
        }
      }
    }
  }

  override fun setupView(view: View, savedInstanceState: Bundle?) {
    startTransitions()

//    TODO: Refresh detail page
//    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(com.hoc.comicapp.R.array.swipe_refresh_colors))

    recycler_chapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chapterAdapter
    }

    setupFab()
    setupMotionLayout()

    switch_mode.isChecked = !args.isDownloaded
    switch_mode.setOnCheckedChangeListener { _, _ ->
      val actionComicDetailFragmentSelf =
        ComicDetailFragmentDirections.actionComicDetailFragmentSelf(
          comic = args.comic,
          title = args.title,
          isDownloaded = !args.isDownloaded
        )
      findNavController().navigate(actionComicDetailFragmentSelf)
    }
  }

  override fun viewIntents(): Observable<ComicDetailIntent> {
    return Observable.mergeArray(
      Observable.just(
        ComicDetailIntent.Initial(args.comic)
      ),
      button_retry
        .clicks()
        .map { ComicDetailIntent.Retry },
//        TODO: Refresh detail page
//        swipe_refresh_layout
//          .refreshes()
//          .map { ComicDetailIntent.Refresh(argComic.link) }
      intentS,
      image_favorite
        .clicks()
        .throttleFirst(300, TimeUnit.MILLISECONDS)
        .map { ComicDetailIntent.ToggleFavorite }
    )
  }
  //endregion
}