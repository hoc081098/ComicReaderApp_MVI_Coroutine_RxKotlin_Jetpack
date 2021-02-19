package com.hoc.comicapp.navigation

import android.content.Context
import androidx.navigation.NavController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

typealias NavigationCommand = NavController.(Context) -> Unit

class AppNavigator {
  private val _commandFlow = Channel<NavigationCommand>(Channel.BUFFERED)

  val commandFlow get() = _commandFlow.receiveAsFlow()

  suspend fun execute(command: NavigationCommand) =
    _commandFlow.send(command).also { Timber.d("Navigate send $command") }
}