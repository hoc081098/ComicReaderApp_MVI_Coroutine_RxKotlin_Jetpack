plugins {
  `comic-app-plugin`
  `android-library`
}

comicApp {
  namespace = "com.hoc.comicapp.utils"
}

dependencies {

  implementation(deps.kotlin.stdlib)
  implementation(deps.kotlin.coroutinesCore)

  implementation(deps.androidX.appCompat)
  implementation(deps.androidX.core)
  implementation(deps.androidX.view.material)

  implementation(deps.timber)
  implementation(deps.koin.android)

  implementation(deps.reactiveX.java)
  implementation(deps.reactiveX.kotlin)
  implementation(deps.reactiveX.android)
  implementation(deps.rxRelay)
  implementation(deps.rxBinding.platform)

  implementation(deps.customView.materialSpinner)
  implementation(deps.customView.materialSearchView)

  implementation(deps.firebase.firestore)
  implementation(platform(deps.firebase.bom))

  implementation(deps.arrow.core)

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
