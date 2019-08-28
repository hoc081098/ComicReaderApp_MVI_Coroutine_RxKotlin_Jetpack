package com.hoc.comicapp.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.*
import androidx.work.*
import com.hoc.comicapp.R
import com.hoc.comicapp.utils.getColorBy
import com.hoc.comicapp.utils.getDrawableBy
import com.hoc.comicapp.utils.textChanges
import com.hoc.comicapp.utils.toast
import com.hoc.comicapp.worker.DownloadComicWorker
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
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

      setTextColor(getColorBy(id = R.color.colorTextOnBackground))
      setBackgroundColor(getColorBy(id = R.color.colorBackground))

      setBackIcon(getDrawableBy(id = R.drawable.ic_keyboard_backspace_white_24dp))
      setCloseIcon(getDrawableBy(id = R.drawable.ic_close_white_24dp))
    }

    testWorker()
  }

  private fun testWorker() {
    val workRequest = OneTimeWorkRequestBuilder<DownloadComicWorker>()
      .setInputData(
        workDataOf(
          DownloadComicWorker.CHAPTER_LINK
              to "https://ww2.mangafox.online/beloved-wife-is-not-well-behaved/episode-69-1071370787200123"
        )
      )
      .setConstraints(
        Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .setRequiresStorageNotLow(true)
          .build()
      )
      .build()

    val workManager = WorkManager.getInstance(this)

    workManager.enqueue(workRequest)

    workManager
      .getWorkInfoByIdLiveData(workRequest.id)
      .observe(this, Observer { workInfo ->
        Timber.d("workInfo = $workInfo")

        if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
          toast("Work finished!")
        }
      })
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

  fun setToolbarTitle(title: CharSequence) {
    supportActionBar?.title = title
  }
}
