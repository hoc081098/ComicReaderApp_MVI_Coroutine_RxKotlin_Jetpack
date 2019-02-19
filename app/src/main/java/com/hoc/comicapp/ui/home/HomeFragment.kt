package com.hoc.comicapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.R
import com.hoc.comicapp.data.models.Comic
import com.hoc.comicapp.observe
import com.hoc.comicapp.observeEvent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.item_recyclerview_top_month_comic.view.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.koin.androidx.viewmodel.ext.android.viewModel

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
      item.run {
        textComicName.text = title
        textChapter.text = chapters.first().chapterName
      }
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

    homeViewModel.state.observe(this) {
      updateTopMonth(it)
    }
    homeViewModel.singleEvent.observeEvent(this) {
      when (it) {
        is HomeSingleEvent.MessageEvent -> {
          Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    homeViewModel.processIntents(
      Observable.mergeArray(
        Observable.just(HomeViewIntent.Initial)
      )
    ).addTo(compositeDisposable)
  }

  override fun onPause() {
    super.onPause()
    compositeDisposable.clear()
  }

  private fun updateTopMonth(it: HomeViewState) {
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
