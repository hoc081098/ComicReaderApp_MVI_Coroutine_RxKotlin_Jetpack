package com.hoc.comicapp.utils

import androidx.collection.LruCache
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
class Cache<K : Any, V : Any>(
  maxSize: Int,
  private val entryLifetime: Duration,
  private val sizeOf: (key: K, value: V) -> Int = DefaultSizeCalculator,
) {
  private val timeSource = TimeSource.Monotonic

  private val cache = object : LruCache<K, Value<V>>(maxSize) {
    override fun sizeOf(key: K, value: Value<V>) = sizeOf(key, value.value)
  }

  @Synchronized operator fun get(key: K): V? {
    val value = cache[key] ?: return null
    return if (value.expiredTime.hasPassedNow()) {
      remove(key)
      null
    } else {
      value.value
    }
  }

  fun remove(key: K) {
    cache.remove(key)
  }

  operator fun set(key: K, value: V) {
    cache.put(
      key,
      Value(
        value,
        timeSource.markNow() + entryLifetime
      )
    )
  }

  private data class Value<V : Any>(
    val value: V,
    val expiredTime: TimeMark,
  )

  private object DefaultSizeCalculator : (Any, Any) -> Int {
    override fun invoke(p1: Any, p2: Any): Int = 1
  }
}
