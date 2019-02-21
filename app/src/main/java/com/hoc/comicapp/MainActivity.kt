package com.hoc.comicapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlin.LazyThreadSafetyMode.NONE

class MainActivity : AppCompatActivity() {

  private val appBarConfiguration: AppBarConfiguration by lazy(NONE) {
    AppBarConfiguration(
      topLevelDestinationIds = setOf(
        R.id.home_fragment_dest
        // TODO: add more...
      ),
      drawerLayout = drawer_layout
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    fab.setOnClickListener { view ->
      Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        .setAction("Action", null).show()
    }

    val navController = findNavController(R.id.main_nav_fragment)
    // Set up action bar
    setupActionBarWithNavController(
      navController,
      appBarConfiguration
    )
    // Set up navigation view menu
    nav_view.setupWithNavController(navController)
  }

  override fun onSupportNavigateUp() =
    findNavController(R.id.main_nav_fragment).navigateUp(appBarConfiguration)

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.main, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return item.onNavDestinationSelected(findNavController(R.id.main_nav_fragment))
      || super.onOptionsItemSelected(item)
  }
}
