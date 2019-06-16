package com.hoc.comicapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.hoc.comicapp.utils.textChanges
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : AppCompatActivity() {

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
      setTextColor(
        ContextCompat.getColor(
          this@MainActivity,
          R.color.colorTextOnBackground
        )
      )
      setBackgroundColor(
        ContextCompat.getColor(
          this@MainActivity,
          R.color.colorBackground
        )
      )
      setBackIcon(
        ContextCompat.getDrawable(
          this@MainActivity,
          R.drawable.ic_keyboard_backspace_white_24dp
        )
      )
      setCloseIcon(
        ContextCompat.getDrawable(
          this@MainActivity,
          R.drawable.ic_close_white_24dp
        )
      )
    }
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
    return item.onNavDestinationSelected(findNavController(R.id.main_nav_fragment))
        || super.onOptionsItemSelected(item)
  }

  fun showSearch() = search_view.showSearch()

  fun hideSearchIfNeeded() = if (search_view.isSearchOpen) search_view.closeSearch() else Unit

  fun textSearchChanges() = search_view.textChanges()
}
