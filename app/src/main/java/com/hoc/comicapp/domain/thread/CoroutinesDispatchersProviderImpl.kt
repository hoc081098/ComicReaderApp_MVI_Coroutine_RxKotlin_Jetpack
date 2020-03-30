package com.hoc.comicapp.domain.thread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx2.asCoroutineDispatcher

class CoroutinesDispatchersProviderImpl(
  private val rxSchedulerProvider: RxSchedulerProvider,
  override val main: CoroutineDispatcher = rxSchedulerProvider.main.asCoroutineDispatcher(),
  override val io: CoroutineDispatcher = rxSchedulerProvider.io.asCoroutineDispatcher(),
) : CoroutinesDispatchersProvider