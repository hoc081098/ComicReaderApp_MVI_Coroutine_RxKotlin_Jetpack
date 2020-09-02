// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  repositories {
    google()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
  }
  dependencies {
    classpath(deps.sdk.classpath)
    classpath(deps.kotlin.classpath)
    classpath(deps.androidX.navigation.classpath)
    classpath(deps.firebase.classpath)
    classpath(deps.firebase.crashlytics.classpath)
  }
}

allprojects {
  repositories {
    google()
    jcenter()
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://jitpack.io")
    maven(url = "http://dl.bintray.com/amulyakhare/maven")
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
  }
}

tasks.register("clean", Delete::class) { delete(rootProject.buildDir) }

