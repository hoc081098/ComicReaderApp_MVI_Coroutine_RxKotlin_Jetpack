package com.hoc.comicapp.koin

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import com.hoc.comicapp.activity.main.MainActivity
import com.hoc.comicapp.navigation.AppNavigator
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val navigationModule = module {
  scope<MainActivity> {
    scoped { (navController: NavController) ->
      AppNavigator(
        navController = navController,
        context = androidContext()
      )
    }
  }
}

@Suppress("NOTHING_TO_INLINE")
inline val Fragment.requireAppNavigator
  get() = (requireActivity() as MainActivity).appNavigator

@Suppress("NOTHING_TO_INLINE")
inline val Fragment.appNavigator
  get() = (activity as? MainActivity)?.appNavigator
