package com.hoc.comicapp.navigation

import androidx.fragment.app.Fragment
import com.hoc.comicapp.activity.main.MainActivity
import org.koin.dsl.module

val navigationModule = module {
  scope<MainActivity> {
    scoped { AppNavigator() }
  }
}

@Suppress("NOTHING_TO_INLINE")
inline val Fragment.appNavigator
  get() = (requireActivity() as MainActivity).appNavigator