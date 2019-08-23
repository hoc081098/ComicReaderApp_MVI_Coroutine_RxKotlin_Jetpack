package com.hoc.comicapp.ui.chapter_detail

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.inflate
import kotlinx.android.synthetic.main.item_recycler_chapter_detail_image.view.*
import timber.log.Timber

object StringDiffUtilItemCallback : DiffUtil.ItemCallback<String>() {
  override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
  override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}

class ChapterImageAdapter(
  private val glide: GlideRequests
) :
  ListAdapter<String, ChapterImageAdapter.VH>(StringDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(parent inflate R.layout.item_recycler_chapter_detail_image)

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val imageChapter = itemView.image_chapter!!
    private val progressBar = itemView.progress_bar!!
    private val buttonRetry = itemView.button_retry!!
    private val groupError = itemView.group_error!!

    init {
      buttonRetry.setOnClickListener {
        val position = adapterPosition
        if (position != RecyclerView.NO_POSITION) {
          loadImage(imageUrl = getItem(position))
        }
      }
    }

    fun bind(imageUrl: String) {
      Timber.d("chapter_detail_state bind $imageUrl")
      loadImage(imageUrl = imageUrl)
    }

    private fun loadImage(imageUrl: String) {
      // show progressBar, hide error
      progressBar.isVisible = true
      groupError.isVisible = false

      // clear image
      imageChapter.setImageDrawable(null)

      glide
        .load(imageUrl)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .fitCenter()
        .listener(object : RequestListener<Drawable?> {
          override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable?>?,
            isFirstResource: Boolean
          ): Boolean {
            // show error, hide progressBar
            progressBar.isVisible = false
            groupError.isVisible = true
            return false
          }

          override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable?>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
          ): Boolean {
            // hide progressBar
            progressBar.isVisible = false
            return false
          }
        })
        .into(imageChapter)
    }
  }
}