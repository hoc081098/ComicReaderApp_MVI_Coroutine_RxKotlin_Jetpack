plugins {
  `comic-app-plugin`
  `android-library`
  id("kotlin-kapt")
  id("androidx.navigation.safeargs.kotlin")
}

comicApp {
  parcelize = true
  namespace = "com.hoc.comicapp.navigation"
}

dependencies {
  implementation(deps.androidX.navigation.fragment)
  implementation(deps.timber)

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
