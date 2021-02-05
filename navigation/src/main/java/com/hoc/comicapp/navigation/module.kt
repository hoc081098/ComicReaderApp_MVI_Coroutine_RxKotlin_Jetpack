package com.hoc.comicapp.navigation

import org.koin.dsl.module

val navigationModule = module {
  single { AppNavigator() }
}