package com.hoc.comicapp.domain.models

import com.hoc.comicapp.domain.models.DownloadProgress.Companion.MAX
import com.hoc.comicapp.domain.models.DownloadProgress.Companion.MIN

/**
 * The download progress, from [MIN] to [MAX] (inclusive).
 */
@JvmInline
value class DownloadProgress private constructor(val value: Int) {
  companion object {
    const val MIN = 0
    const val MAX = 100

    @Throws(IllegalArgumentException::class)
    fun require(progress: Int): DownloadProgress {
      require(progress in MIN..MAX) { "progress must be in $MIN..$MAX, but it was $progress" }
      return DownloadProgress(progress)
    }
  }
}
