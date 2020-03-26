package com.hoc.comicapp.utils

import androidx.collection.LruCache
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@ExperimentalTime
class Cache<K : Any, V : Any>(maxSize: Int, private val entryLifetime: Duration) {
  private val timeSource = TimeSource.Monotonic
  private val cache = LruCache<K, Value<V>>(maxSize)

  operator fun get(key: K): V? {
    return synchronized(this) {
      val value = cache[key] ?: return null
      if (value.expiredTime.hasPassedNow()) {
        remove(key)
        null
      } else {
        value.value
      }
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

  private companion object {
    data class Value<V : Any>(
      val value: V,
      val expiredTime: TimeMark
    )
  }
}