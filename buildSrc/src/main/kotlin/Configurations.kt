@file:Suppress("ClassName", "SpellCheckingInspection")

object versions {
  object kotlin {
    const val core = "1.4.0"
    const val coroutines = "1.3.9"
  }

  object androidX {
    const val activity = "1.2.0-alpha08"
    const val appCompat = "1.3.0-alpha02"
    const val core = "1.5.0-alpha02"
    const val fragment = "1.3.0-alpha08"
    const val startUp = "1.0.0-alpha03"

    object view {
      const val constraintLayout = "2.0.1"
      const val material = "1.2.0"
      const val recyclerView = "1.2.0-alpha05"
    }

    const val navigation = "2.3.0"
    const val lifecycle = "2.3.0-alpha07"
    const val room = "2.3.0-alpha02"
  }
}

object deps {
  object kotlin {
    const val coroutinesAndroid =
      "org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.kotlin.coroutines}"
    const val coroutinesCore =
      "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.kotlin.coroutines}"
    const val coroutinesPlayServices =
      "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${versions.kotlin.coroutines}"
    const val coroutinesRx3 =
      "org.jetbrains.kotlinx:kotlinx-coroutines-rx3:${versions.kotlin.coroutines}"
    const val stdlib = "org.jetbrains.kotlin:stdlib-jdk8:${versions.kotlin.core}"
  }

  object androidX {
    const val activity = "androidx.activity:activity-ktx:${versions.androidX.activity}"
    const val appCompat = "androidx.appcompat:appcompat:${versions.androidX.appCompat}"
    const val core = "androidx.core:core-ktx:${versions.androidX.core}"
    const val fragment = "androidx.fragment:fragment-ktx:${versions.androidX.fragment}"
    const val startUp = "androidx.startup:startup-runtime:${versions.androidX.startUp}"

    object view {
      const val constraintLayout =
        "androidx.constraintlayout:constraintlayout:${versions.androidX.view.constraintLayout}"
      const val material = "com.google.android.material:material:${versions.androidX.view.material}"
      const val recyclerView =
        "androidx.recyclerview:recyclerview:${versions.androidX.view.recyclerView}"
    }

    object navigation {
      const val fragment =
        "androidx.navigation:navigation-fragment-ktx:${versions.androidX.navigation}"
      const val ui = "androidx.navigation:navigation-ui-ktx:${versions.androidX.navigation}"
    }

    object lifecycle {
      const val viewModel =
        "androidx.lifecycle:lifecycle-viewmodel-ktx:${versions.androidX.lifecycle}"
      const val liveData =
        "androidx.lifecycle:lifecycle-livedata-ktx:${versions.androidX.lifecycle}"
      const val reactiveStreams =
        "androidx.lifecycle:lifecycle-reactivestreams-ktx:${versions.androidX.lifecycle}"
      const val common = "androidx.lifecycle:lifecycle-common-java8:${versions.androidX.lifecycle}"
    }

    object room {
      const val runtime = "androidx.room:room-runtime:${versions.androidX.room}"
      const val ktx = "androidx.room:room-ktx:${versions.androidX.room}"
      const val rxJava3 = "androidx.room:room-rxjava3:${versions.androidX.room}"
      const val compiler = "androidx.room:room-compiler:${versions.androidX.room}"
    }
  }
}

