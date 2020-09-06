package com.hoc.comicapp.ui.category

import android.util.SparseBooleanArray
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.ItemRecyclerCategoryBinding
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflater
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxbinding4.view.detaches
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo

object CategoryDiffUtilItemCallback : DiffUtil.ItemCallback<Category>() {
  override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
}

class CategoryAdapter(
  private val glide: GlideRequests,
  private val compositeDisposable: CompositeDisposable,
) : ListAdapter<Category, CategoryAdapter.VH>(CategoryDiffUtilItemCallback) {
  private val collapsedStatus = SparseBooleanArray()
  private val clickCategoryS = PublishRelay.create<Category>()
  val clickCategoryObservable get() = clickCategoryS.asObservable()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(
      ItemRecyclerCategoryBinding.inflate(
        parent.inflater,
        parent,
        false
      ),
      parent
    )

  override fun onBindViewHolder(holder: VH, position: Int) =
    holder.bind(getItem(position), position)

  inner class VH(private val binding: ItemRecyclerCategoryBinding, parent: ViewGroup) :
    RecyclerView.ViewHolder(binding.root) {
    init {
      Observable
        .mergeArray(
          binding.imageNavigationNext.clicks(),
          binding.textGoToDetail.clicks(),
          itemView.clicks()
        ).takeUntil(parent.detaches())
        .map { bindingAdapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .map { getItem(it) }
        .subscribe(clickCategoryS)
        .addTo(compositeDisposable)
    }

    fun bind(item: Category, position: Int) = binding.run {
      glide
        .load(item.thumbnail)
        .placeholder(R.drawable.splash_background)
        .thumbnail(0.5f)
        .fitCenter()
        .into(imageCategoryThumbnail)

      textCategoryName.text = item.name
      textCategoryDescription.setText(
        item.description,
        collapsedStatus,
        position
      )
    }
  }
}
