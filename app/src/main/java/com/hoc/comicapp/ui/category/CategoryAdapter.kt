package com.hoc.comicapp.ui.category

import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.Category
import com.hoc.comicapp.utils.asObservable
import com.hoc.comicapp.utils.inflate
import com.jakewharton.rxbinding3.view.clicks
import com.jakewharton.rxbinding3.view.detaches
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.item_recycler_category.view.*

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
    VH(parent inflate R.layout.item_recycler_category, parent)

  override fun onBindViewHolder(holder: VH, position: Int) =
    holder.bind(getItem(position), position)

  inner class VH(itemView: View, parent: ViewGroup) : RecyclerView.ViewHolder(itemView) {
    private val textCategoryName = itemView.text_category_name!!
    private val textCategoryDescription = itemView.text_category_description!!
    private val imageCategoryThumbnail = itemView.image_category_thumbnail!!

    init {
      Observable.mergeArray(
          itemView.image_navigation_next.clicks(),
          itemView.text_go_to_detail.clicks(),
          itemView.clicks()
        ).takeUntil(parent.detaches())
        .map { adapterPosition }
        .filter { it != RecyclerView.NO_POSITION }
        .map { getItem(it) }
        .subscribe(clickCategoryS)
        .addTo(compositeDisposable)
    }

    fun bind(item: Category, position: Int) {
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