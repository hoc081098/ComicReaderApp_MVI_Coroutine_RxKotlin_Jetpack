package com.hoc.comicapp.domain.thread

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

class RxSchedulerProviderImpl(
  override val main: Scheduler = AndroidSchedulers.mainThread(),
  override val io: Scheduler = Schedulers.io(),
) : RxSchedulerProvider
