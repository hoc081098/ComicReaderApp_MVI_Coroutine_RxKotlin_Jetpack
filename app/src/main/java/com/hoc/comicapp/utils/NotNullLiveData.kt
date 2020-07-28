package com.hoc.comicapp.utils

import androidx.lifecycle.LiveData

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