@file:Suppress("ClassName", "SpellCheckingInspection", "MemberVisibilityCanBePrivate")

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object appConfig {
  const val applicationId = "com.hoc.comicapp"

  private const val MAJOR = 0
  private const val MINOR = 0
  private const val PATCH = 3
  const val versionCode = MAJOR * 10_000 + MINOR * 100 + PATCH
  val versionName by lazy {
    val date = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now())
    "$MAJOR.$MINOR.$PATCH-SNAPSHOT $date"
  }

  val supportedLocales = setOf("en")
}

object versions {
  const val ktLint = "0.44.0"

  object sdk {
    const val buildTools = "32.0.0"
    const val compile = 32
    const val min = 23
    const val target = 32
  }

  object kotlin {
    const val core = "1.6.21"
    const val coroutines = "1.6.1"
  }

  object androidX {
    const val activity = "1.4.0"
    const val appCompat = "1.4.1"
    const val core = "1.7.0"
    const val fragment = "1.4.1"
    const val startUp = "1.1.1"

    object view {
      const val constraintLayout = "2.1.3"
      const val material = "1.4.0"
      const val recyclerView = "1.3.0-alpha02"
    }

    const val navigation = "2.4.2"
    const val lifecycle = "2.4.1"
    const val room = "2.5.0-alpha01"
    const val work = "2.8.0-alpha02"
  }

  const val koin = "3.2.0"
  const val moshiKotlin = "1.13.0"
  const val retrofit = "2.9.0"
  const val okHttpLoggingInterceptor = "5.0.0-alpha.2"
  const val leakCanaryAndroid = "2.7"
  const val rxRelay = "3.0.1"
  const val rxBinding = "4.0.0"
  const val timber = "5.0.1"

  object reactiveX {
    const val kotlin = "3.0.1"
    const val java = "3.1.2"
    const val android = "3.0.0"
  }

  const val glide = "4.12.0"

  object customView {
    const val materialSearchView = "1.4.0"
    const val expandableTextView = "0.1.4"
    const val materialSpinner = "1.3.1"
    const val circularProgressIndicator = "1.3.0"
    const val photoView = "2.3.0"
    const val swipeRevealLayout = "1.4.1"
    const val circleImageView = "3.1.0"
    const val textDrawable = "1.0.1"
  }

  object firebase {
    const val bom = "28.4.2"
  }

  const val arrow = "1.1.2"
  const val uri = "0.3.3"
  const val viewBindingDelegate = "1.3.1"
  const val flowExt = "0.3.0"
}

object deps {
  object module {
    const val baseUi = ":base-ui"
    const val utils = ":utils"
    const val navigation = ":navigation"
    const val domain = ":domain"
    const val featureHome = ":feature-home"
  }

  object kotlin {
    const val coroutinesAndroid = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${versions.kotlin.coroutines}"
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${versions.kotlin.coroutines}"
    const val coroutinesPlayServices = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${versions.kotlin.coroutines}"
    const val coroutinesRx3 = "org.jetbrains.kotlinx:kotlinx-coroutines-rx3:${versions.kotlin.coroutines}"
    const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${versions.kotlin.core}"
  }

  object androidX {
    const val activity = "androidx.activity:activity-ktx:${versions.androidX.activity}"
    const val appCompat = "androidx.appcompat:appcompat:${versions.androidX.appCompat}"
    const val core = "androidx.core:core-ktx:${versions.androidX.core}"
    const val fragment = "androidx.fragment:fragment-ktx:${versions.androidX.fragment}"
    const val startUp = "androidx.startup:startup-runtime:${versions.androidX.startUp}"

    object view {
      const val constraintLayout = "androidx.constraintlayout:constraintlayout:${versions.androidX.view.constraintLayout}"
      const val material = "com.google.android.material:material:${versions.androidX.view.material}"
      const val recyclerView = "androidx.recyclerview:recyclerview:${versions.androidX.view.recyclerView}"
    }

    object navigation {
      const val fragment = "androidx.navigation:navigation-fragment-ktx:${versions.androidX.navigation}"
      const val ui = "androidx.navigation:navigation-ui-ktx:${versions.androidX.navigation}"
    }

    object lifecycle {
      const val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:${versions.androidX.lifecycle}"
      const val liveData = "androidx.lifecycle:lifecycle-livedata-ktx:${versions.androidX.lifecycle}"
      const val reactiveStreams = "androidx.lifecycle:lifecycle-reactivestreams-ktx:${versions.androidX.lifecycle}"
      const val common = "androidx.lifecycle:lifecycle-common-java8:${versions.androidX.lifecycle}"
    }

    object room {
      const val runtime = "androidx.room:room-runtime:${versions.androidX.room}"
      const val ktx = "androidx.room:room-ktx:${versions.androidX.room}"
      const val rxJava3 = "androidx.room:room-rxjava3:${versions.androidX.room}"
      const val compiler = "androidx.room:room-compiler:${versions.androidX.room}"
    }

    object work {
      const val runtimeKtx = "androidx.work:work-runtime-ktx:${versions.androidX.work}"  // Kotlin + coroutines
    }
  }

  object koin {
    const val android = "io.insert-koin:koin-android:${versions.koin}"
    const val core = "io.insert-koin:koin-core:${versions.koin}"
  }

  const val moshiKotlin = "com.squareup.moshi:moshi-kotlin:${versions.moshiKotlin}"

  object retrofit {
    const val retrofit = "com.squareup.retrofit2:retrofit:${versions.retrofit}"
    const val converterMoshi = "com.squareup.retrofit2:converter-moshi:${versions.retrofit}"
  }

  const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${versions.okHttpLoggingInterceptor}"
  const val leakCanaryAndroid = "com.squareup.leakcanary:leakcanary-android:${versions.leakCanaryAndroid}"
  const val rxRelay = "com.jakewharton.rxrelay3:rxrelay:${versions.rxRelay}"

  object rxBinding {
    const val platform = "com.jakewharton.rxbinding4:rxbinding:${versions.rxBinding}"
    const val core = "com.jakewharton.rxbinding4:rxbinding-core:${versions.rxBinding}"
    const val material = "com.jakewharton.rxbinding4:rxbinding-material:${versions.rxBinding}"
    const val swipeRefreshLayout = "com.jakewharton.rxbinding4:rxbinding-swiperefreshlayout:${versions.rxBinding}"
    const val recyclerView = "com.jakewharton.rxbinding4:rxbinding-recyclerview:${versions.rxBinding}"
  }

  const val timber = "com.jakewharton.timber:timber:${versions.timber}"

  object reactiveX {
    const val kotlin = "io.reactivex.rxjava3:rxkotlin:${versions.reactiveX.kotlin}"
    const val java = "io.reactivex.rxjava3:rxjava:${versions.reactiveX.java}"
    const val android = "io.reactivex.rxjava3:rxandroid:${versions.reactiveX.android}"
  }

  object glide {
    const val glide = "com.github.bumptech.glide:glide:${versions.glide}"
    const val compiler = "com.github.bumptech.glide:compiler:${versions.glide}"
    const val integration = "com.github.bumptech.glide:okhttp3-integration:${versions.glide}"
  }

  object customView {
    const val materialSearchView = "com.miguelcatalan:materialsearchview:${versions.customView.materialSearchView}"
    const val expandableTextView = "com.ms-square:expandableTextView:${versions.customView.expandableTextView}"
    const val materialSpinner = "com.jaredrummler:material-spinner:${versions.customView.materialSpinner}"
    const val circularProgressIndicator = "com.github.antonKozyriatskyi:CircularProgressIndicator:${versions.customView.circularProgressIndicator}"
    const val photoView = "com.github.chrisbanes:PhotoView:${versions.customView.photoView}"
    const val swipeRevealLayout = "com.chauthai.swipereveallayout:swipe-reveal-layout:${versions.customView.swipeRevealLayout}"
    const val circleImageView = "de.hdodenhof:circleimageview:${versions.customView.circleImageView}"
    const val textDrawable = "com.amulyakhare:com.amulyakhare.textdrawable:${versions.customView.textDrawable}"
  }

  object firebase {
    const val bom = "com.google.firebase:firebase-bom:${versions.firebase.bom}"
    const val auth = "com.google.firebase:firebase-auth-ktx"
    const val storage = "com.google.firebase:firebase-storage-ktx"
    const val firestore = "com.google.firebase:firebase-firestore-ktx"
    const val analytics = "com.google.firebase:firebase-analytics-ktx"
    const val crashlytics = "com.google.firebase:firebase-crashlytics-ktx"
  }

  object arrow {
    const val core = "io.arrow-kt:arrow-core:${versions.arrow}"
    const val fxCoroutines = "io.arrow-kt:arrow-fx-coroutines:${versions.arrow}"
  }

  object uri {
    const val core = "com.chrynan.uri:uri-core:${versions.uri}"
  }

  const val listenableFuture = "com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava"
  const val flowExt = "io.github.hoc081098:FlowExt:${versions.flowExt}"
  const val viewBindingDelegate = "com.github.hoc081098:ViewBindingDelegate:${versions.viewBindingDelegate}"
}

