package com.hoc.comicapp.domain.thread

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.rx3.asCoroutineDispatcher

class CoroutinesDispatchersProviderImpl(
  rxSchedulerProvider: RxSchedulerProvider
) : CoroutinesDispatchersProvider {
  override val main: CoroutineDispatcher = rxSchedulerProvider.main.asCoroutineDispatcher()
  override val io: CoroutineDispatcher = rxSchedulerProvider.io.asCoroutineDispatcher()
}
