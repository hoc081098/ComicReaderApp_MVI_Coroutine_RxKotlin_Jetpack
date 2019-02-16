package com.hoc.comicapp

import kotlinx.coroutines.CoroutineDispatcher

interface CoroutinesDispatcherProvider {
  val ui: CoroutineDispatcher
  val io: CoroutineDispatcher
}