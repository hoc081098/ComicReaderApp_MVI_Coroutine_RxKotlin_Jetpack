package com.hoc.comicapp.navigation

import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import androidx.navigation.NavController
import timber.log.Timber

typealias NavigationCommand = NavController.(Context) -> Unit

class AppNavigator(
  private val navController: NavController,
  private val context: Context,
) {
  @MainThread
  fun execute(command: NavigationCommand) {
    check(Looper.getMainLooper() == Looper.myLooper()) {
      "Expected to be called on the main thread but was " + Thread.currentThread().name
    }

    /**
     * Performs a navigation on the [NavController] using the provided [directions] and [navigatorExtras],
     * catching any [IllegalArgumentException] which usually happens when users trigger (e.g. click)
     * navigation multiple times very quickly on slower devices.
     * For more context, see [stackoverflow](https://stackoverflow.com/questions/51060762/illegalargumentexception-navigation-destination-xxx-is-unknown-to-this-navcontr).
     */
    Timber.d("Navigate send $command")
    try {
      navController.command(context)
    } catch (e: IllegalStateException) {
      Timber.e(e, "Execute navigation command error: $e")
    }
  }
}
