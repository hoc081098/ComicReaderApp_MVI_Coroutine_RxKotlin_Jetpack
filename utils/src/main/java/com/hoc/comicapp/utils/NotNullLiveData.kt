package com.hoc.comicapp.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.reactivex.rxjava3.core.Observable

open class NotNullLiveData<T : Any>(value: T) : LiveData<T>(value) {
  override fun getValue(): T = super.getValue()!!

  @Suppress("RedundantOverride")
  override fun setValue(value: T) = super.setValue(value)

  @Suppress("RedundantOverride")
  override fun postValue(value: T) = super.postValue(value)
}

class NotNullMutableLiveData<T : Any>(value: T) : NotNullLiveData<T>(value) {
  public override fun setValue(value: T) = super.setValue(value)

  public override fun postValue(value: T) = super.postValue(value)
}

inline fun <T : Any> NotNullLiveData<T>.observe(
  owner: LifecycleOwner,
  crossinline observer: (T) -> Unit,
) = Observer { value: T -> observer(value) }.also { observe(owner, it) }

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Any> NotNullLiveData<T>.toObservable(): Observable<T> {
  @Suppress("UNCHECKED_CAST")
  return (this as LiveData<T?>).toObservable { error("Should not reach here!") }
}
