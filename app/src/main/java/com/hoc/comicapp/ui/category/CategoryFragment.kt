package com.hoc.comicapp.ui.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import io.reactivex.Observable
import kotlinx.android.synthetic.main.fragment_category.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class CategoryFragment : Fragment() {
  private val viewModel by viewModel<CategoryViewModel>()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
    inflater.inflate(R.layout.fragment_category, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    initView()
    bind()
  }

  private fun initView() {

  }

  private fun bind() {
    viewModel.singleEvent.observeEvent(owner = viewLifecycleOwner) {

    }
    viewModel.state.observe(owner = viewLifecycleOwner) {
      textttt.text = it.toString()
    }
    viewModel.processIntents(
      Observable.mergeArray(
        Observable.just(CategoryViewIntent.Initial)
      )
    )
  }
}