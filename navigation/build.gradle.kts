plugins {
    `comic-app-plugin`
    `android-library`
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

comicApp { parcelize = true }

dependencies {
    implementation(deps.androidX.navigation.fragment)
    implementation(deps.kotlin.coroutinesCore)

    testImplementation("junit:junit:4.13")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}