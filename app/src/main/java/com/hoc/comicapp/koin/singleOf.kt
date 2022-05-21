package com.hoc.comicapp.koin

import org.koin.core.annotation.KoinInternalApi
import org.koin.core.definition.BeanDefinition
import org.koin.core.module.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.module._singleInstanceFactory
import org.koin.core.module.dsl.setupInstance

// TODO(koin): https://github.com/InsertKoinIO/koin/issues/1342
@OptIn(KoinInternalApi::class)
inline fun <reified R, reified T1, reified T2, reified T3, reified T4, reified T5, reified T6, reified T7, reified T8, reified T9, reified T10, reified T11> Module.singleOf(
  crossinline constructor: (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11) -> R,
  options: BeanDefinition<R>.() -> Unit
): KoinDefinition<R> = setupInstance(
  _singleInstanceFactory(definition = {
    constructor(
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
    )
  }),
  options
)
