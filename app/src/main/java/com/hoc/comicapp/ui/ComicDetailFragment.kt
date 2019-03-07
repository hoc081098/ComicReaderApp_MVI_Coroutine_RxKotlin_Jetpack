package com.hoc.comicapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.toast

class ComicDetailFragment : Fragment() {
  private val args by navArgs<ComicDetailFragmentArgs>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.fragment_comic_detail, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val comic = args.comic
    requireContext().toast(comic.title)
  }
}