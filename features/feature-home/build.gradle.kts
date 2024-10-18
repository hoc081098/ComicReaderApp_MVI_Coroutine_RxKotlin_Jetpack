plugins {
  `android-library`
  `comic-app-plugin`
}

comicApp {
  namespace = "com.hoc.comicapp.features.home"
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
