plugins {
  `comic-app-plugin`
  `android-library`
//  kotlin
}

dependencies {
  implementation(deps.reactiveX.java)
  implementation(deps.kotlin.coroutinesCore)

  implementation(deps.arrow.core)

  implementation(deps.androidX.lifecycle.liveData)
}
