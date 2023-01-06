plugins {
  `comic-app-plugin`
  `android-library`
}

comicApp {
  viewBinding = true
  namespace = "com.hoc.comicapp.base"
}

dependencies {
  implementation(project(deps.module.utils))

  implementation(deps.kotlin.stdlib)

  implementation(deps.androidX.core)
  implementation(deps.androidX.appCompat)

  implementation(deps.reactiveX.java)
  implementation(deps.reactiveX.kotlin)
  implementation(deps.timber)

  implementation(deps.koin.android)

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
