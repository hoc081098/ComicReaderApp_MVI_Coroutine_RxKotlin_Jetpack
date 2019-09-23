import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by rootProject.extra
val navVersion: String by rootProject.extra
val coroutinesVersion: String by rootProject.extra
val lifecycleVersion: String by rootProject.extra
val pagingVersion: String by rootProject.extra
val koinVersion: String by rootProject.extra
val materialVersion: String by rootProject.extra
val glideVersion: String by rootProject.extra
val rxBindingVersion: String by rootProject.extra
val timberVersion: String by rootProject.extra
val rxRelayVersion: String by rootProject.extra
val threetenabpVersion: String by rootProject.extra
val retrofit2Version: String by rootProject.extra
val roomVersion: String by rootProject.extra
val workVersion: String by rootProject.extra

plugins {
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-android-extensions")
  id("kotlin-kapt")
  id("androidx.navigation.safeargs.kotlin")
}

androidExtensions { isExperimental = true }

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

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  implementation(kotlin("stdlib-jdk8", kotlinVersion))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-rx2:$coroutinesVersion")

  implementation("androidx.appcompat:appcompat:1.1.0")
  implementation("androidx.core:core-ktx:1.2.0-alpha04")
  implementation("androidx.activity:activity-ktx:1.1.0-alpha03")
  implementation("androidx.fragment:fragment-ktx:1.2.0-alpha03")
  implementation("androidx.constraintlayout:constraintlayout:2.0.0-beta2")

  implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
  implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

  implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")

  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
  implementation("androidx.lifecycle:lifecycle-reactivestreams-ktx:$lifecycleVersion")
  implementation("com.shopify:livedata-ktx:3.0.0")

  implementation("com.google.android.material:material:$materialVersion")

  implementation("androidx.room:room-runtime:$roomVersion")
  kapt("androidx.room:room-compiler:$roomVersion") // For Kotlin use kapt instead of annotationProcessor
  implementation("androidx.room:room-ktx:$roomVersion") // optional - Kotlin Extensions and Coroutines support for Room
  implementation("androidx.room:room-rxjava2:$roomVersion") // optional - RxJava support for Room

  implementation("androidx.work:work-runtime-ktx:$workVersion") // Kotlin + coroutines

  implementation("org.koin:koin-androidx-viewmodel:$koinVersion")

  implementation("com.squareup.moshi:moshi-kotlin:1.8.0")
  implementation("com.squareup.retrofit2:retrofit:$retrofit2Version")
  implementation("com.squareup.retrofit2:converter-moshi:$retrofit2Version")
  implementation("com.squareup.okhttp3:logging-interceptor:3.13.1")
  debugImplementation("com.squareup.leakcanary:leakcanary-android:2.0-alpha-2")

  implementation("com.jakewharton.rxrelay2:rxrelay:$rxRelayVersion")
  implementation("com.jakewharton.threetenabp:threetenabp:$threetenabpVersion")
  implementation("com.jakewharton.rxbinding3:rxbinding:$rxBindingVersion")
  implementation("com.jakewharton.rxbinding3:rxbinding-core:$rxBindingVersion")
  implementation("com.jakewharton.rxbinding3:rxbinding-material:$rxBindingVersion")
  implementation("com.jakewharton.rxbinding3:rxbinding-swiperefreshlayout:$rxBindingVersion")
  implementation("com.jakewharton.rxbinding3:rxbinding-recyclerview:$rxBindingVersion")
  implementation("com.jakewharton.timber:timber:$timberVersion")

  implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
  implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

  implementation("com.github.bumptech.glide:glide:$glideVersion")
  kapt("com.github.bumptech.glide:compiler:$glideVersion")

  implementation("com.miguelcatalan:materialsearchview:1.4.0")
  implementation("com.ms-square:expandableTextView:0.1.4")
  implementation("com.jaredrummler:material-spinner:1.3.1")
  implementation("com.github.antonKozyriatskyi:CircularProgressIndicator:1.3.0")
  implementation("com.github.chrisbanes:PhotoView:2.3.0")

  testImplementation("junit:junit:4.12")
  androidTestImplementation("androidx.test:runner:1.1.2-alpha01")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.1.2-alpha01")
}
