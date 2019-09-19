package com.hoc.comicapp.domain.thread

import com.hoc.comicapp.domain.thread.RxSchedulerProvider
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class RxSchedulerProviderImpl(
  override val main: Scheduler = AndroidSchedulers.mainThread(),
  override val io: Scheduler = Schedulers.io()
) : RxSchedulerProvider