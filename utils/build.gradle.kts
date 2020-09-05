plugins {
  id("com.android.library")
  id("kotlin-android")
}

android {
  compileSdkVersion(versions.sdk.compile)
  buildToolsVersion(versions.sdk.buildTools)

  defaultConfig {
    minSdkVersion(versions.sdk.min)
    targetSdkVersion(versions.sdk.target)

    versionCode = appConfig.versionCode
    versionName = appConfig.versionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

dependencies {

  implementation(deps.kotlin.stdlib)
  implementation(deps.kotlin.coroutinesCore)

  implementation(deps.androidX.appCompat)
  implementation(deps.androidX.core)
  implementation(deps.androidX.view.material)

  implementation(deps.timber)
  implementation(deps.koin.androidXScope)
  implementation(deps.koin.androidXViewModel)

  implementation(deps.reactiveX.java)
  implementation(deps.reactiveX.kotlin)
  implementation(deps.reactiveX.android)
  implementation(deps.rxRelay)
  implementation(deps.rxBinding.platform)

  implementation(deps.customView.materialSpinner)
  implementation(deps.customView.materialSearchView)

  implementation(deps.firebase.firestore)

  testImplementation("junit:junit:4.13")
  androidTestImplementation("androidx.test.ext:junit:1.1.2")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}