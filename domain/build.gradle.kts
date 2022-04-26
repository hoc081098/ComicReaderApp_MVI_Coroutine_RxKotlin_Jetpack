plugins {
  `comic-app-plugin`
  kotlin
}

dependencies {
  implementation(deps.reactiveX.java)
  implementation(deps.kotlin.coroutinesCore)
  implementation(deps.arrow.core)
  api(deps.uri.core)
}
