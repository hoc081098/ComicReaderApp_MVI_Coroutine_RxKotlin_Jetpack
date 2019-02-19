package com.hoc.comicapp

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.shopify.livedataktx.LiveDataKtx
import com.shopify.livedataktx.MutableLiveDataKtx

@MainThread
fun <T> MutableLiveDataKtx<T>.setValueIfNew(newValue: T) {
  if (value != newValue) {
    value = newValue
  }
}

inline fun <T> LiveDataKtx<T>.observe(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit
) = Observer<T?> { it?.let { observer(it) } }.also { observe(owner, it) }


inline fun <T> LiveData<Event<T>>.observeEvent(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit
) = Observer { event: Event<T>? ->
  event?.getContentIfNotHandled()?.let(observer)
}.also { observe(owner, it) }
