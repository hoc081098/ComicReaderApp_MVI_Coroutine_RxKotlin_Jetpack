package com.hoc.comicapp.activity.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.main.MainContract.ViewIntent
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.getColorBy
import com.hoc.comicapp.utils.getDrawableBy
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.showAlertDialog
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.textChanges
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : AppCompatActivity() {
  private val mainVM by viewModel<MainVM>()
  private val compositeDisposable = CompositeDisposable()

  private val glide by lazy(NONE) { GlideApp.with(this) }

  private val appBarConfiguration: AppBarConfiguration by lazy(NONE) {
    AppBarConfiguration(
      topLevelDestinationIds = setOf(R.id.home_fragment_dest),
      drawerLayout = drawer_layout
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    val navController = findNavController(R.id.main_nav_fragment)
    // Set up action bar
    setupActionBarWithNavController(
      navController,
      appBarConfiguration
    )
    // Set up navigation view menu
    nav_view.setupWithNavController(navController)
    nav_view.bringToFront()

    search_view.run {
      setHint("Search comic...")

      setTextColor(getColorBy(id = R.color.colorTextOnBackground))
      setBackgroundColor(getColorBy(id = R.color.colorBackground))

      setBackIcon(getDrawableBy(id = R.drawable.ic_keyboard_backspace_white_24dp))
      setCloseIcon(getDrawableBy(id = R.drawable.ic_close_white_24dp))
    }

    bindVM()
  }

  private fun bindVM() {
    val headerView = nav_view.getHeaderView(0)
    val textDisplayName = headerView.text_display_name!!
    val textEmail = headerView.text_email!!
    val imageAvatar = headerView.image_avatar!!
    val imageView = headerView.imageView!!
    val userAccountGroup = headerView.user_account_group!!

    val loginMenuItem = nav_view.menu.findItem(R.id.action_home_fragment_dest_to_loginFragment)!!
    val logoutMenuItem = nav_view.menu.findItem(R.id.action_logout)!!

    val mainContent = findViewById<View>(android.R.id.content)!!

    mainVM.state.observe(owner = this) { (user, isLoading, error) ->
      nav_view.menu.setGroupVisible(R.id.group2, !isLoading)

      if (user === null) {
        loginMenuItem.isVisible = true
        logoutMenuItem.isVisible = false
        userAccountGroup.isVisible = false
        imageView.isVisible = true
      } else {
        loginMenuItem.isVisible = false
        logoutMenuItem.isVisible = true
        userAccountGroup.isVisible = true
        imageView.isVisible = false

        textDisplayName.text = user.displayName
        textEmail.text = user.email
        glide
          .load(user.photoURL)
          .centerCrop()
          .placeholder(R.drawable.person_white_96x96)
          .error(R.drawable.person_white_96x96)
          .diskCacheStrategy(DiskCacheStrategy.ALL)
          .into(imageAvatar)
      }
    }

    mainVM.singleEvent.observeEvent(owner = this) { event ->
      when (event) {
        is MainContract.SingleEvent.GetUserError -> {
          mainContent.snack("Get user error: ${event.error.getMessage()}")
        }
        MainContract.SingleEvent.SignOutSuccess -> {
          mainContent.snack("Sign out success")
        }
        is MainContract.SingleEvent.SignOutFailure -> {
          mainContent.snack("Sign out error: ${event.error.getMessage()}")
        }
      }
    }

    mainVM
      .processIntents(
        Observable.mergeArray(
          Observable.just(ViewIntent.Initial),
          logoutMenuItem
            .clicks()
            .doOnNext { drawer_layout.closeDrawer(GravityCompat.START) }
            .exhaustMap { showDeleteComicDialog() }
            .map { ViewIntent.SignOut }
        )
      )
      .addTo(compositeDisposable)
  }


  private fun showDeleteComicDialog(): Observable<Unit> {
    return Observable.create<Unit> { emitter ->
      val alertDialog = showAlertDialog {
        title("Sign out")
        message("Are you sure want to sign out?")
        cancelable(true)
        iconId(R.drawable.ic_exit_to_app_white_24dp)

        negativeAction("Cancel") { dialog, _ ->
          dialog.cancel()
          if (!emitter.isDisposed) {
            emitter.onComplete()
          }
        }
        positiveAction("OK") { dialog, _ ->
          dialog.dismiss()
          if (!emitter.isDisposed) {
            emitter.onNext(Unit)
            emitter.onComplete()
          }
        }
        onCancel {
          if (!emitter.isDisposed) {
            emitter.onComplete()
          }
        }
      }
      emitter.setCancellable { alertDialog.dismiss() }
    }
  }


  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
  }

  override fun onSupportNavigateUp() =
    findNavController(R.id.main_nav_fragment).navigateUp(appBarConfiguration)

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {

    return true
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.main, menu)
    return true
  }

  override fun onBackPressed() {
    if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
      drawer_layout.closeDrawer(GravityCompat.START)
    } else {
      if (search_view.isSearchOpen) {
        search_view.closeSearch()
      } else {
        super.onBackPressed()
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.searchComicFragment &&
      findNavController(R.id.main_nav_fragment).currentDestination?.id == R.id.searchComicFragment
    ) {
      return showSearch().let { true }
    }
    return item.onNavDestinationSelected(findNavController(R.id.main_nav_fragment))
        || super.onOptionsItemSelected(item)
  }

  fun showSearch() = search_view.showSearch()

  fun hideSearchIfNeeded() = if (search_view.isSearchOpen) search_view.closeSearch() else Unit

  fun textSearchChanges() = search_view.textChanges()

  fun setToolbarTitle(title: CharSequence) {
    supportActionBar?.title = title
  }
}
