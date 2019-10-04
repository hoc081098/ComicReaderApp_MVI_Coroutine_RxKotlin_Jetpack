package com.hoc.comicapp

private enum class Test1 {
  a, b, c
}

private enum class Test2(val a: Comparator<Int>) {
  aa(compareBy { it }), bb(compareByDescending { it })
}

fun main() {
  println(Test1.values() === Test1.values())
  println(Test1.values().contentEquals(Test1.values()))
  println(Test2.values() === Test2.values())
  println(Test2.values().contentEquals(Test2.values()))
}