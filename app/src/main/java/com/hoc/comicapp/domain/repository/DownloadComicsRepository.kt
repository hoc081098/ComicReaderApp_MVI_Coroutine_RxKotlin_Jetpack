package com.hoc.comicapp.domain.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

interface DownloadComicsRepository {
  @ExperimentalCoroutinesApi
  fun downloadChapter(chapterLink: String): Flow<Int>
}