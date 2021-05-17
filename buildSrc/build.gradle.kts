repositories {
  mavenCentral()
  google()
  jcenter()
  gradlePluginPortal()
  maven(url = "https://plugins.gradle.org/m2/")
}

plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

gradlePlugin {
  plugins {
    register("comic-app-plugin") {
      id = "comic-app-plugin"
      implementationClass = "com.hoc.comicapp.plugin.ComicAppPlugin"
    }
  }
}

object PluginVersions {
  const val kotlin = "1.5.0"
  const val androidGradle = "7.0.0-alpha15"
  const val navigationSafeArgs = "2.3.1"
  const val googleServices = "4.3.4"
  const val crashlytics = "2.4.1"
  const val spotless = "5.12.4"
}

object Plugins {
  const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${PluginVersions.kotlin}"
  const val androidGradle = "com.android.tools.build:gradle:${PluginVersions.androidGradle}"
  const val navigationSafeArgs = "androidx.navigation:navigation-safe-args-gradle-plugin:${PluginVersions.navigationSafeArgs}"
  const val googleServices = "com.google.gms:google-services:${PluginVersions.googleServices}"
  const val crashlytics = "com.google.firebase:firebase-crashlytics-gradle:${PluginVersions.crashlytics}"
  const val spotless = "com.diffplug.spotless:spotless-plugin-gradle:${PluginVersions.spotless}"
}

dependencies {
  implementation(Plugins.androidGradle)
  implementation(Plugins.kotlin)

  implementation(Plugins.spotless)
  implementation(Plugins.navigationSafeArgs)

  implementation(Plugins.googleServices)
  implementation(Plugins.crashlytics)

  implementation("com.github.ben-manes:gradle-versions-plugin:0.31.0")
}