package com.hoc.comicapp.domain.thread

import kotlinx.coroutines.CoroutineDispatcher

interface CoroutinesDispatchersProvider {
  val main: CoroutineDispatcher
  val io: CoroutineDispatcher
}
