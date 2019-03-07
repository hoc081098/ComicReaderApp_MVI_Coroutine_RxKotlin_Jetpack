import org.jetbrains.kotlin.gradle.internal.AndroidExtensionsExtension

val kotlinVersion: String by rootProject.extra
val navVersion: String by rootProject.extra
val coroutinesVersion: String by rootProject.extra
val lifecycleVersion: String by rootProject.extra
val pagingVersion: String by rootProject.extra

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-android-extensions")
  id("kotlin-kapt")
  id("androidx.navigation.safeargs.kotlin")
}

androidExtensions {
  configure(delegateClosureOf<AndroidExtensionsExtension> {
    isExperimental = true
  })
}

android {
  compileSdkVersion(28)

  defaultConfig {
    applicationId = "com.hoc.comicapp"
    minSdkVersion(21)
    targetSdkVersion(28)
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  implementation(kotlin("stdlib-jdk8", kotlinVersion))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")

  implementation("androidx.appcompat:appcompat:1.1.0-alpha02")
  implementation("androidx.core:core-ktx:1.1.0-alpha04")
  implementation("androidx.constraintlayout:constraintlayout:1.1.3")

  implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
  implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

  implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")

  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-livedata:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycleVersion")
  implementation("com.shopify:livedata-ktx:3.0.0")

  implementation("com.google.android.material:material:1.1.0-alpha03")

  implementation("org.koin:koin-androidx-viewmodel:1.0.2")

  implementation("com.squareup.moshi:moshi-kotlin:1.8.0")
  implementation("com.squareup.retrofit2:retrofit:2.5.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.5.0")
  implementation("com.squareup.okhttp3:logging-interceptor:3.13.1")

  implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
  implementation("com.jakewharton.rxrelay2:rxrelay:2.1.0")
  implementation("com.jakewharton.threetenabp:threetenabp:1.1.2")
  implementation("com.jakewharton.rxbinding3:rxbinding:3.0.0-alpha2")
  implementation("com.jakewharton.rxbinding3:rxbinding-core:3.0.0-alpha2")
  implementation("com.jakewharton.rxbinding3:rxbinding-material:3.0.0-alpha2")
  implementation("com.jakewharton.rxbinding3:rxbinding-swiperefreshlayout:3.0.0-alpha2")
  implementation("com.jakewharton.rxbinding3:rxbinding-recyclerview:3.0.0-alpha2")
  implementation("com.jakewharton.timber:timber:4.7.1")

  implementation("io.reactivex.rxjava2:rxkotlin:2.3.0")
  implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

  implementation("com.github.bumptech.glide:glide:4.9.0")
  kapt("com.github.bumptech.glide:compiler:4.9.0")

  testImplementation("junit:junit:4.12")
  androidTestImplementation("androidx.test:runner:1.1.2-alpha01")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.1.2-alpha01")
}
