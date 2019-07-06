package com.hoc.comicapp.domain.thread

import io.reactivex.Scheduler

interface RxSchedulerProvider {
  val main: Scheduler
  val io: Scheduler
}