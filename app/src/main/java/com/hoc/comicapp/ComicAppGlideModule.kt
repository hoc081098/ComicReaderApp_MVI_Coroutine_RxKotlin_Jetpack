package com.hoc.comicapp

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class ComicAppGlideModule : AppGlideModule() {
  override fun applyOptions(context: Context, builder: GlideBuilder) {
    val diskCacheSizeBytes = 200 * 1024 * 1024L // 200 MB

    builder.setDiskCache(
      InternalCacheDiskCacheFactory(
        context,
        diskCacheSizeBytes
      )
    )
  }
}