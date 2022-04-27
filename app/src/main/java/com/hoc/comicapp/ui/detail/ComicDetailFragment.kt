package com.hoc.comicapp.ui.detail

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.transition.MaterialContainerTransform
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.base.BaseFragment
import com.hoc.comicapp.databinding.FragmentComicDetailBinding
import com.hoc.comicapp.koin.requireAppNavigator
import com.hoc.comicapp.navigation.Arguments
import com.hoc.comicapp.ui.detail.ComicDetailIntent.CancelDownloadChapter
import com.hoc.comicapp.ui.detail.ComicDetailIntent.DeleteChapter
import com.hoc.comicapp.ui.detail.ComicDetailIntent.DownloadChapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.Chapter
import com.hoc.comicapp.ui.detail.ComicDetailViewState.ComicDetail
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloaded
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.Downloading
import com.hoc.comicapp.ui.detail.ComicDetailViewState.DownloadState.NotYetDownload
import com.hoc.comicapp.utils.action
import com.hoc.comicapp.utils.dismissAlertDialog
import com.hoc.comicapp.utils.getDrawableBy
import com.hoc.comicapp.utils.isOrientationPortrait
import com.hoc.comicapp.utils.showAlertDialog
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.startPostponedEnterTransitionWhenDrawn
import com.hoc.comicapp.utils.themeColor
import com.hoc.comicapp.utils.themeInterpolator
import com.hoc.comicapp.utils.unit
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.recyclerview.scrollEvents
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.withLatestFrom
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.absoluteValue

class ComicDetailFragment : BaseFragment<
  ComicDetailIntent,
  ComicDetailViewState,
  ComicDetailSingleEvent,
  ComicDetailViewModel
  >(R.layout.fragment_comic_detail) {
  override val viewModel by viewModel<ComicDetailViewModel> {
    parametersOf(args.isDownloaded)
  }
  override val viewBinding by viewBinding<FragmentComicDetailBinding> {
    recyclerChapters.adapter = null
    rootDetail.setTransitionListener(null)
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
    requireActivity().dismissAlertDialog()
  }

  //region Setup view
  private fun prepareTransitions() {
    sharedElementEnterTransition = MaterialContainerTransform().apply {
      // Animate behind status bar.
      drawingViewId = R.id.main_nav_fragment
      duration = resources.getInteger(R.integer.reply_motion_default_large).toLong()

      setAllContainerColors(requireContext().themeColor(R.attr.colorSurface))

      scrimColor = Color.TRANSPARENT
      interpolator = requireContext().themeInterpolator(R.attr.motionInterpolatorPersistent)
    }
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
      .into(viewBinding.imageThumbnail)
  }

  private fun setupFab() {
    val fab = viewBinding.fab

    val scrollEvents = viewBinding.recyclerChapters.scrollEvents().share()
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
          .let { viewBinding.recyclerChapters.layoutManager!!.startSmoothScroll(it) }
      }
      .addTo(compositeDisposable)
  }

  private fun setupMotionLayout() {
    viewBinding.rootDetail
      .getConstraintSet(R.layout.fragment_comic_detail)
      .setGuidelinePercent(
        R.id.guideline,
        if (requireContext().isOrientationPortrait) 0.45f else 0.175f
      )

    viewBinding.rootDetail
      .getConstraintSet(R.layout.fragment_comic_detail_end)
      .setGuidelinePercent(
        R.id.guideline,
        if (requireContext().isOrientationPortrait) 0.175f else 0.05f
      )

    var lastProgress = 0f
    viewBinding.rootDetail.setTransitionListener(
      object : TransitionAdapter() {
        override fun onTransitionChange(
          motionLayout: MotionLayout?,
          startId: Int,
          endId: Int,
          progress: Float,
        ) {
          if (progress - lastProgress > 0) {
// from start to end
            if ((progress - 1f).absoluteValue < 0.5f) {
              viewBinding.textTitle.maxLines = 1
              viewBinding.textLastUpdatedStatusView.maxLines = 2
              Timber.d("END")
            }
          } else {
// from end to start
            if (progress < 0.3f) {
              viewBinding.textTitle.maxLines = 6
              viewBinding.textLastUpdatedStatusView.maxLines = Int.MAX_VALUE
              Timber.d("START")
            }
          }
          lastProgress = progress
        }
      }
    )
  }
  //endregion

  //region Handle adapter click event
  private fun onClickChapter(chapter: Chapter, view: View) {
    when (view.id) {
      R.id.image_download -> onClickDownload(chapter)
      else -> {
        requireAppNavigator.execute {
          navigate(
            ComicDetailFragmentDirections.actionComicDetailFragmentToChapterDetailFragment(
              chapter = chapter.toChapterDetailArgs(),
              isDownloaded = args.isDownloaded
            )
          )
        }
      }
    }
  }

  private fun onClickChapterChip(category: ComicDetailViewState.Category) {
    requireAppNavigator.execute {
      val toCategoryDetailFragment =
        ComicDetailFragmentDirections.actionComicDetailFragmentToCategoryDetailFragment(
          title = category.name,
          category = Arguments.CategoryDetailArgs(
            description = "",
            link = category.link,
            name = category.name,
            thumbnail = ""
          )
        )
      navigate(toCategoryDetailFragment)
    }
  }

  private fun onClickDownload(chapter: Chapter) {
    // TODO: cancel worker???
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
      ComicDetailViewState.DownloadState.Loading -> Unit
    }
  }

  private fun onClickButtonRead(readFirst: Boolean) {
    val comicDetail = viewModel.state.value.comicDetail as? ComicDetail.Detail ?: return
    val chapter =
      comicDetail.chapters.let { if (readFirst) it.lastOrNull() else it.firstOrNull() }
    if (chapter === null) {
      view?.snack("Chapters list is empty!")
    } else {
      requireAppNavigator.execute {
        navigate(
          ComicDetailFragmentDirections.actionComicDetailFragmentToChapterDetailFragment(
            chapter = chapter.toChapterDetailArgs(),
            isDownloaded = args.isDownloaded
          )
        )
      }
    }
  }
  //endregion

  //region Override BaseFragment
  override fun render(viewState: ComicDetailViewState) = viewBinding.run {
    Timber.d("state=$viewState")
    Timber.d("favorite=${viewState.isFavorited}")

    imageFavorite.setImageDrawable(
      when (viewState.isFavorited) {
        true -> requireContext().getDrawableBy(id = R.drawable.ic_favorite_white_24dp)
        false -> requireContext().getDrawableBy(id = R.drawable.ic_favorite_border_white_24dp)
        null -> null
      }
    )

    if (viewState.isLoading) {
      progressBar.visibility = View.VISIBLE
      textLastUpdatedStatusView.text = "Loading..."
    } else {
      progressBar.visibility = View.INVISIBLE
    }

    if (viewState.errorMessage === null) {
      groupError.visibility = View.GONE
    } else {
      groupError.visibility = View.VISIBLE
      textErrorMessage.text = viewState.errorMessage
      textLastUpdatedStatusView.text = "Error occurred"
    }

//    TODO: Refresh detail page
//    if (!viewState.isRefreshing) {
//      swipe_refresh_layout.isRefreshing = false
//    }

    when (val detail = viewState.comicDetail ?: return) {
      is ComicDetail.Detail -> {
        textTitle.text = detail.title

        val list = mutableListOf(
          "Last updated" to detail.lastUpdated,
          "View" to detail.view
        )
        textLastUpdatedStatusView.text = HtmlCompat.fromHtml(
          list.joinToString("<br>") { "\u2022 <b>${it.first}:</b> ${it.second}" },
          HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        loadThumbnail(detail.thumbnail)

        chapterAdapter.submitList(
          listOf(
            ChapterAdapterItem.Header(
              categories = detail.categories,
              shortenedContent = detail.shortenedContent
            )
          ) + detail.chapters.map { ChapterAdapterItem.Chapter(it) } + ChapterAdapterItem.Dummy
        )
      }
      is ComicDetail.Initial -> {
        textTitle.text = detail.title
        loadThumbnail(detail.thumbnail)
      }
    }

    startPostponedEnterTransitionWhenDrawn()
  }

  @Suppress("IMPLICIT_CAST_TO_ANY")
  override fun handleEvent(event: ComicDetailSingleEvent) {
    return when (event) {
      is ComicDetailSingleEvent.MessageEvent -> {
        view?.snack(event.message)
      }
      is ComicDetailSingleEvent.EnqueuedDownloadSuccess -> {
        view?.snack("Enqueued download ${event.chapter.chapterName}") {
          action("View") {
            requireAppNavigator.execute { navigate(R.id.downloadingChaptersFragment) }
          }
        }
      }
      is ComicDetailSingleEvent.EnqueuedDownloadFailure -> {
        view?.snack("Failed to enqueue download '${event.chapter.chapterName}'")
      }
      is ComicDetailSingleEvent.DeletedChapter -> Unit
      is ComicDetailSingleEvent.DeleteChapterError -> Unit
    }.unit
  }

  override fun setupView(view: View, savedInstanceState: Bundle?) = viewBinding.run {
    Timber.d("transitionName: ${args.transitionName}")
    viewBinding.rootDetail.transitionName = args.transitionName
    postponeEnterTransition()

//    TODO: Refresh detail page
//    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(com.hoc.comicapp.R.array.swipe_refresh_colors))

    recyclerChapters.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context)
      adapter = chapterAdapter
    }

    setupFab()
    setupMotionLayout()

    switchMode.isChecked = !args.isDownloaded
    switchMode.setOnCheckedChangeListener { _, _ ->
      lifecycleScope.launch {
        requireAppNavigator.execute {
          val actionComicDetailFragmentSelf =
            ComicDetailFragmentDirections.actionComicDetailFragmentSelf(
              comic = args.comic,
              title = args.title,
              isDownloaded = !args.isDownloaded
            )
          navigate(actionComicDetailFragmentSelf)
        }
      }
    }
  }

  override fun viewIntents(): Observable<ComicDetailIntent> = viewBinding.run {
    Observable.mergeArray(
      Observable.just(
        ComicDetailIntent.Initial(args.comic)
      ),
      buttonRetry
        .clicks()
        .map { ComicDetailIntent.Retry },
//        TODO: Refresh detail page
//        swipe_refresh_layout
//          .refreshes()
//          .map { ComicDetailIntent.Refresh(argComic.link) }
      intentS,
      imageFavorite
        .clicks()
        .throttleFirst(300, TimeUnit.MILLISECONDS)
        .map { ComicDetailIntent.ToggleFavorite }
    )
  }
  //endregion
}

private fun Chapter.toChapterDetailArgs(): Arguments.ChapterDetailArgs {
  return Arguments.ChapterDetailArgs(
    chapterLink = chapterLink,
    chapterName = chapterName,
    time = time,
    view = view,
    comicLink = comicLink,
  )
}
