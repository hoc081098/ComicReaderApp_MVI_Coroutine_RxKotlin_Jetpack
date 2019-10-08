package com.hoc.comicapp

import io.reactivex.Observable
import io.reactivex.rxkotlin.blockingSubscribeBy
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.rx2.collect
import kotlinx.coroutines.rx2.rxObservable
import java.util.concurrent.TimeUnit

fun main() {
  val observable = rxObservable<Long> {
    send(0)

    println("----")

    Observable
      .interval(10, TimeUnit.MILLISECONDS)
      .doOnSubscribe { println("on subscribe") }
      .doOnDispose { println("on dispose") }
      .collect { send(it) }

    println("***")
  }
  println("1")
  Thread.sleep(1000)
  println("2")
  val disposable = observable.subscribeBy { println("Sub >> $it") }
  Thread.sleep(10_000)
  disposable.dispose()
  println("3")
  Thread.sleep(10_000)
}