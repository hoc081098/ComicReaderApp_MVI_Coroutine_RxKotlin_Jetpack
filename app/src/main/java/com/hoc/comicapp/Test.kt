package com.hoc.comicapp

import io.reactivex.rxkotlin.subscribeBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.withContext

suspend fun f(): Int {
  return withContext(Dispatchers.IO) {
    println("IO" + Thread.currentThread())
    2
  }
}

@ExperimentalCoroutinesApi
fun main() {
  val dis = flow<Int> {

    emit(0)

    emit(f())

    delay(1000)
    emit(4)

    delay(1000)
    emit(6)

    delay(1000)
    emit(8)

    delay(1000)
    emit(10)
  }
    .flowOn(Dispatchers.Default)
    .asObservable()
    .subscribeBy { println("$it ${Thread.currentThread()}") }

  println("[111]")
  Thread.sleep(2000)
  dis.dispose()
  println("[222]")
  println("dispose")
  Thread.sleep(3000)
  println("[333]")
}