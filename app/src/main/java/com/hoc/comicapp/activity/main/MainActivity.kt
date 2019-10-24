package com.hoc.comicapp.activity.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.hoc.comicapp.R
import com.hoc.comicapp.activity.main.MainContract.ViewIntent
import com.hoc.comicapp.utils.getColorBy
import com.hoc.comicapp.utils.getDrawableBy
import com.hoc.comicapp.utils.observe
import com.hoc.comicapp.utils.observeEvent
import com.hoc.comicapp.utils.textChanges
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : AppCompatActivity() {
  private val vm by viewModel<MainVM>()
  private val compositeDisposable = CompositeDisposable()

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
    vm.state.observe(owner = this) {
      Timber.d("State=$it")
    }

    vm.singleEvent.observeEvent(owner = this) {
      Timber.d("Event=$it")
    }

    vm
      .processIntents(
        Observable.just(
          ViewIntent.Initial
        )
      )
      .addTo(compositeDisposable)
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
