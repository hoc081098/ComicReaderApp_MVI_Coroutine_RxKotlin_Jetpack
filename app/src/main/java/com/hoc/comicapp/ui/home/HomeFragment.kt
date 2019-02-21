package com.hoc.comicapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.hoc.comicapp.*
import com.hoc.comicapp.data.models.Comic
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.item_recyclerview_top_month_comic.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class ComicDiffUtilItemCallback : DiffUtil.ItemCallback<Comic>() {
  override fun areItemsTheSame(oldItem: Comic, newItem: Comic): Boolean {
    return oldItem.link == newItem.link
  }

  override fun areContentsTheSame(oldItem: Comic, newItem: Comic): Boolean {
    return oldItem == newItem
  }
}

class TopMonthAdapter :
  ListAdapter<Comic, TopMonthAdapter.VH>(ComicDiffUtilItemCallback()) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return LayoutInflater.from(parent.context)
      .inflate(R.layout.item_recyclerview_top_month_comic, parent, false)
      .let(::VH)
  }

  override fun onBindViewHolder(holder: VH, position: Int) {
    holder.bind(getItem(position))
  }

  class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageComic = itemView.image_comic!!
    private val textComicName = itemView.text_comic_name!!
    private val textChapter = itemView.text_chapter!!

    fun bind(item: Comic) {
      val circularProgressDrawable = CircularProgressDrawable(itemView.context).apply {
        strokeWidth = 5f
        centerRadius = 30f
        start()
      }

      textComicName.text = item.title
      textChapter.text = item.chapters.first().chapterName
      GlideApp
        .with(itemView.context)
        .load(item.thumbnail)
        .fitCenter()
        .placeholder(circularProgressDrawable)
        .into(imageComic)
    }
  }
}

@ExperimentalCoroutinesApi
class HomeFragment : Fragment() {
  private val homeViewModel by viewModel<HomeViewModel>()
  private val topMonthAdapter = TopMonthAdapter()
  private val compositeDisposable = CompositeDisposable()


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_home, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    recycler_top_month.run {
      setHasFixedSize(true)
      layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
      adapter = topMonthAdapter
    }
    swipe_refresh_layout.setColorSchemeColors(*resources.getIntArray(R.array.swipe_refresh_colors))

    homeViewModel.state.observe(this) {
      updateTopMonth(it)
    }
    homeViewModel.singleEvent.observeEvent(this) {
      when (it) {
        is HomeSingleEvent.MessageEvent -> {
          view.snack(it.message)
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    homeViewModel.processIntents(
      Observable.mergeArray(
        Observable.just(HomeViewIntent.Initial),
        swipe_refresh_layout.refreshes().map { HomeViewIntent.Refresh }
      )
    ).addTo(compositeDisposable)
  }

  override fun onPause() {
    super.onPause()
    compositeDisposable.clear()
  }

  private fun updateTopMonth(it: HomeViewState) {
    Timber
      .tag("###")
      .d("top_month_state=[${it.topMonthLoading}, ${it.topMonthErrorMessage}, ${it.topMonthComics}]")

    if (it.topMonthLoading) {
      top_month_progress_bar.visibility = View.VISIBLE
    } else {
      top_month_progress_bar.visibility = View.INVISIBLE
    }
    if (it.topMonthErrorMessage != null) {
      top_month_error_message.visibility = View.VISIBLE
      top_month_error_message.text = it.topMonthErrorMessage
    } else {
      top_month_error_message.visibility = View.INVISIBLE
    }
    topMonthAdapter.submitList(it.topMonthComics)
  }
}

