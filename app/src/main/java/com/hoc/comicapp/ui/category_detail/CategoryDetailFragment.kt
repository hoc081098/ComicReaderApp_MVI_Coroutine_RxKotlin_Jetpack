package com.hoc.comicapp.ui.category_detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.ui.category_detail.CategoryDetailContract.ViewIntent
import com.hoc.comicapp.utils.observe
import io.reactivex.Observable.just
import io.reactivex.Observable.mergeArray
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_category_detail.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CategoryDetailFragment : Fragment() {
  private val args by navArgs<CategoryDetailFragmentArgs>()

  private val vm by viewModel<CategoryDetailVM>()
  private val compositeDisposable = CompositeDisposable()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ) = inflater.inflate(R.layout.fragment_category_detail, container, false)!!

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val categoryDetailAdapter = CategoryDetailAdapter(
      GlideApp.with(this),
      viewLifecycleOwner
    )
    recycler_category_detail.run {
      layoutManager = GridLayoutManager(context, 2).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
          override fun getSpanSize(position: Int): Int {
            return if (categoryDetailAdapter.getItemViewType(position) == R.layout.item_recycler_category_detail_comic) {
              1
            } else {
              2
            }
          }
        }
      }
      setHasFixedSize(true)
      adapter = categoryDetailAdapter
    }

    vm.state.observe(owner = viewLifecycleOwner) { (items, isRefreshing) ->
      categoryDetailAdapter.submitList(items)
    }
    vm.processIntents(
      mergeArray(
        just(ViewIntent.Initial(args.category))
      )
    ).addTo(compositeDisposable)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    compositeDisposable.clear()
  }
}