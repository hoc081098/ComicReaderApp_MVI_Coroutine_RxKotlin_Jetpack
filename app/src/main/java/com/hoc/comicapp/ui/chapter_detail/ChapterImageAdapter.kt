package com.hoc.comicapp.ui.chapter_detail

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hoc.comicapp.GlideRequest
import com.hoc.comicapp.GlideRequests
import com.hoc.comicapp.R
import com.hoc.comicapp.databinding.ItemRecyclerChapterDetailImageBinding
import com.hoc.comicapp.utils.inflater
import timber.log.Timber
import java.io.File

object StringDiffUtilItemCallback : DiffUtil.ItemCallback<String>() {
  override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
  override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}

class ChapterImageAdapter(
  private val glide: GlideRequests,
) :
  ListAdapter<String, ChapterImageAdapter.VH>(StringDiffUtilItemCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    VH(
      ItemRecyclerChapterDetailImageBinding.inflate(
        parent.inflater,
        parent,
        false
      )
    )

  override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

  inner class VH(private val binding: ItemRecyclerChapterDetailImageBinding) :
    RecyclerView.ViewHolder(binding.root) {
    init {
      binding.buttonRetry.setOnClickListener {
        val position = bindingAdapterPosition
        if (position != RecyclerView.NO_POSITION) {
          loadImage(imageUrl = getItem(position))
        }
      }
    }

    fun bind(imageUrl: String) {
      Timber.d("chapter_detail_state bind $imageUrl")
      loadImage(imageUrl = imageUrl)
    }

    private fun loadImage(imageUrl: String) = binding.run {
      val file = File(itemView.context.filesDir, imageUrl)
      when {
        file.exists() -> loadLocal(file)
        else -> loadRemote(imageUrl)
      }
        .listener(
          object : RequestListener<Drawable?> {
            override fun onLoadFailed(
              e: GlideException?,
              model: Any?,
              target: Target<Drawable?>?,
              isFirstResource: Boolean,
            ): Boolean {
              // show error, hide progressBar
              groupError.isVisible = true
              progressBar.isVisible = false

              return false
            }

            override fun onResourceReady(
              resource: Drawable?,
              model: Any?,
              target: Target<Drawable?>?,
              dataSource: DataSource?,
              isFirstResource: Boolean,
            ): Boolean {
              // hide progressBar, hide error
              progressBar.isVisible = false
              groupError.isVisible = false

              return false
            }
          }
        )
        .transition(DrawableTransitionOptions.withCrossFade())
        .dontTransform()
        .placeholder(R.drawable.splash_background)
        .error(R.drawable.splash_background)
        .into(imageChapter)
    }

    private fun loadRemote(imageUrl: String): GlideRequest<Drawable> = binding.run {
      Timber.d("load_chapter [remote] $imageUrl")

      // show progressBar, hide error
      progressBar.isVisible = true
      groupError.isVisible = false

      // load image url from remote
      glide
        .load(imageUrl)
        .thumbnail(0.5f)
        .format(DecodeFormat.PREFER_RGB_565)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
    }

    private fun loadLocal(file: File): GlideRequest<Drawable> = binding.run {
      Timber.d("load_chapter [local] $file")

      // load a local file, don't need show progress bar
      // hide progressBar, hide error
      progressBar.isVisible = false
      groupError.isVisible = false

      glide
        .load(file)
        .thumbnail(0.5f)
        .format(DecodeFormat.PREFER_RGB_565)
    }
  }
}
