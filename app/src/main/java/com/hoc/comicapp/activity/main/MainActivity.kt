package com.hoc.comicapp.activity.main

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hoc.comicapp.GlideApp
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.main.MainContract.ViewIntent
import com.hoc.comicapp.activity.main.MainContract.ViewState.User
import com.hoc.comicapp.databinding.ActivityMainBinding
import com.hoc.comicapp.domain.models.getMessage
import com.hoc.comicapp.navigation.AppNavigator
import com.hoc.comicapp.utils.dismissAlertDialog
import com.hoc.comicapp.utils.dpToPx
import com.hoc.comicapp.utils.exhaustMap
import com.hoc.comicapp.utils.getColorBy
import com.hoc.comicapp.utils.getDrawableBy
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.showAlertDialogAsMaybe
import com.hoc.comicapp.utils.snack
import com.hoc.comicapp.utils.textChanges
import com.hoc081098.viewbindingdelegate.viewBinding
import com.jakewharton.rxbinding4.view.clicks
import de.hdodenhof.circleimageview.CircleImageView
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import org.koin.androidx.scope.ScopeActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : ScopeActivity(R.layout.activity_main) {
  /**
   * Get [AppNavigator].
   * Should only be called on the main thread.
   */
  val appNavigator by lazy(NONE) {
    scope
      .get<AppNavigator> { parametersOf(navController) }
      .also { Timber.d("appNavigator: $it") }
  }
  private val navController by lazy(NONE) {
    findNavController(R.id.main_nav_fragment)
      .also { Timber.d("navController: $it") }
  }

  private val mainVM by viewModel<MainVM>()
  private val viewBinding by viewBinding<ActivityMainBinding>()
  private val compositeDisposable = CompositeDisposable()

  private val glide by lazy(NONE) { GlideApp.with(this) }

  private val appBarConfiguration: AppBarConfiguration by lazy(NONE) {
    AppBarConfiguration(
      topLevelDestinationIds = setOf(R.id.home_fragment_dest),
      drawerLayout = viewBinding.drawerLayout
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setSupportActionBar(viewBinding.toolbar)

    // Set up action bar
    setupActionBarWithNavController(
      navController,
      appBarConfiguration
    )
    // Set up navigation view menu
    viewBinding.navView.setupWithNavController(navController)
    viewBinding.navView.bringToFront()

    viewBinding.searchView.run {
      setHint("Search comic...")

      setTextColor(getColorBy(id = R.color.colorTextOnBackground))
      setBackgroundColor(getColorBy(id = R.color.colorBackground))

      setBackIcon(getDrawableBy(id = R.drawable.ic_keyboard_backspace_white_24dp))
      setCloseIcon(getDrawableBy(id = R.drawable.ic_close_white_24dp))
    }

    bindVM()

    if (savedInstanceState !== null) {
      dismissAlertDialog()
    }
  }

  private fun bindVM() {
    val navView = viewBinding.navView

    val headerView = navView.getHeaderView(0)
    val textDisplayName = headerView.findViewById<TextView>(R.id.text_display_name)
    val textEmail = headerView.findViewById<TextView>(R.id.text_email)
    val imageAvatar = headerView.findViewById<CircleImageView>(R.id.image_avatar)
    val imageView = headerView.findViewById<ImageView>(R.id.imageView)
    val userAccountGroup = headerView.findViewById<Group>(R.id.user_account_group)

    val loginMenuItem = navView.menu.findItem(R.id.action_home_fragment_dest_to_loginFragment)!!
    val logoutMenuItem = navView.menu.findItem(R.id.action_logout)!!
    val favoriteMenuItem =
      navView.menu.findItem(R.id.action_home_fragment_dest_to_favoriteComicsFragment)!!

    var prevUser: User? = null
    mainVM.state.observe(owner = this) { (user, isLoading, error) ->
      Timber.d("User = $user, isLoading = $isLoading, error = $error")

      navView.menu.setGroupVisible(R.id.group2, !isLoading)

      if (prevUser === null || prevUser != user) {
        Timber.d("Updated with user = $user")
        prevUser = user

        if (user === null) {
          // not logged in
          // show login, hide logout, hide user account, show comic image
          loginMenuItem.isVisible = true
          logoutMenuItem.isVisible = false
          favoriteMenuItem.isVisible = false
          userAccountGroup.isVisible = false
          imageView.isVisible = true
        } else {
          // user already logged in
          // hide login, show logout, show user account, hide comic image
          loginMenuItem.isVisible = false
          logoutMenuItem.isVisible = true
          favoriteMenuItem.isVisible = true
          userAccountGroup.isVisible = true
          imageView.isVisible = false

          // update user account header
          textDisplayName.text = user.displayName
          textEmail.text = user.email

          when {
            user.photoURL.isNotBlank() -> {
              glide
                .load(user.photoURL)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(ColorDrawable(getColorBy(id = R.color.colorCardBackground)))
                .dontAnimate()
                .into(imageAvatar)
            }
            else -> {
              when (val firstLetter = user.displayName.firstOrNull()) {
                null -> ColorDrawable(getColorBy(id = R.color.colorCardBackground))
                else -> {
                  val size = dpToPx(64).also { Timber.d("64dp = ${it}px") }
                  TextDrawable
                    .builder()
                    .beginConfig()
                    .width(size)
                    .height(size)
                    .endConfig()
                    .buildRect(
                      firstLetter.uppercase(),
                      ColorGenerator.MATERIAL.getColor(user.email)
                    )
                }
              }.let(imageAvatar::setImageDrawable)
            }
          }
        }
      }
    }

    val mainContent = findViewById<View>(android.R.id.content)!!
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
            .doOnNext { viewBinding.drawerLayout.closeDrawer(GravityCompat.START) }
            .exhaustMap { showSignOutDialog() }
            .map { ViewIntent.SignOut }
        )
      )
      .addTo(compositeDisposable)
  }

  private fun showSignOutDialog(): Observable<Unit> {
    return showAlertDialogAsMaybe {
      title("Sign out")
      message("Are you sure want to sign out?")
      cancelable(true)
      iconId(R.drawable.ic_exit_to_app_white_24dp)
    }.toObservable()
  }

  override fun onDestroy() {
    super.onDestroy()
    compositeDisposable.clear()
    dismissAlertDialog()
  }

  override fun onSupportNavigateUp() =
    navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.main, menu)
    return true
  }

  override fun onBackPressed() = viewBinding.run {
    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START)
    } else {
      if (searchView.isSearchOpen) {
        searchView.closeSearch()
      } else {
        super.onBackPressed()
      }
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.searchComicFragment &&
      navController.currentDestination?.id == R.id.searchComicFragment
    ) {
      return showSearch().let { true }
    }
    return item.onNavDestinationSelected(navController) ||
      super.onOptionsItemSelected(item)
  }

  fun showSearch() = viewBinding.searchView.showSearch()

  fun hideSearchIfNeeded() =
    viewBinding.searchView.run { if (isSearchOpen) closeSearch() else Unit }

  fun textSearchChanges() = viewBinding.searchView.textChanges()

  fun setToolbarTitle(title: CharSequence) {
    supportActionBar?.title = title
  }
}
