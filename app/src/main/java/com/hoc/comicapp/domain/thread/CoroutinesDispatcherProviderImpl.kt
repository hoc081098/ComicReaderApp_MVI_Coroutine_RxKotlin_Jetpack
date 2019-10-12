package com.hoc.comicapp.domain.thread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx2.asCoroutineDispatcher

class CoroutinesDispatcherProviderImpl(
  private val rxSchedulerProvider: RxSchedulerProvider,
  override val ui: CoroutineDispatcher = rxSchedulerProvider.main.asCoroutineDispatcher(),
  override val io: CoroutineDispatcher = rxSchedulerProvider.io.asCoroutineDispatcher()
) : CoroutinesDispatcherProvider