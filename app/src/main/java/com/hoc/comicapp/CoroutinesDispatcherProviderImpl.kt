package com.hoc.comicapp

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class CoroutinesDispatcherProviderImpl(
  override val ui: CoroutineDispatcher = Dispatchers.Main,
  override val io: CoroutineDispatcher = Dispatchers.IO
) : CoroutinesDispatcherProvider