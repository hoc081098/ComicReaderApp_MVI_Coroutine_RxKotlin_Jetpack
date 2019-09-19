package com.hoc.comicapp

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

suspend fun f(): Int {
  return withContext(Dispatchers.IO) {
    println("IO" + Thread.currentThread())
    2
  }
}

/*
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
}*/

suspend fun f1(i: Int): Int {
  println(">>>$i")
  delay(2000)
  return i
}

@ExperimentalCoroutinesApi
fun main() {
  runBlocking {
    //    measureTimeMillis {
//      flowOf(1, 2, 3, 4, 5)
//        .flatMapMerge { i ->
//          flow {
//            emit(f1(i))
//          }
//        }
//        .collect { println("## $it") }
//    }.let { println(it) }


    flow {
      for (i in 0..10) {
        emit(i)
        delay(200)
        if (i == 5) throw IllegalStateException("???")
      }
    }.map { it }
      .collect { println(it) }
  }
}