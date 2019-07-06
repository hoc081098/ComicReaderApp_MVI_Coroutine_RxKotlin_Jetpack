package com.hoc.comicapp.domain.thread

import kotlinx.coroutines.CoroutineDispatcher

interface CoroutinesDispatcherProvider {
  val ui: CoroutineDispatcher
  val io: CoroutineDispatcher
}