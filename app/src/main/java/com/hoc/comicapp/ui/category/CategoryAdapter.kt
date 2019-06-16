package com.hoc.comicapp.ui.category

import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hoc.comicapp.R
import com.hoc.comicapp.domain.models.Category
import kotlinx.android.synthetic.main.item_recycler_category.view.*

object CategoryDiffUtilItemCallback : DiffUtil.ItemCallback<Category>() {
  override fun areItemsTheSame(oldItem: Category, newItem: Category) = oldItem.link == newItem.link
  override fun areContentsTheSame(oldItem: Category, newItem: Category) = oldItem == newItem
}

class CategoryAdapter : ListAdapter<Category, CategoryAdapter.VH>(CategoryDiffUtilItemCallback) {
  private val collapsedStatus = SparseBooleanArray()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return LayoutInflater.from(parent.context)
      .inflate(R.layout.item_recycler_category, parent, false)
      .let { VH(it) }
  }

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position), position)

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textCategoryName = itemView.text_category_name!!
    private val textCategoryDescription = itemView.text_category_description!!

    fun bind(item: Category, position: Int) {
      textCategoryName.text = item.name
      textCategoryDescription.setText(
        item.description,
        collapsedStatus,
        position
      )
    }
  }
}