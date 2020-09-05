// Top-level build file where you can add configuration options common to all sub-projects/modules.

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    google()
    jcenter()
    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
    maven(url = "https://plugins.gradle.org/m2/")
  }
  dependencies {
    classpath(deps.sdk.classpath)
    classpath(deps.kotlin.classpath)
    classpath(deps.androidX.navigation.classpath)
    classpath(deps.firebase.classpath)
    classpath(deps.firebase.crashlytics.classpath)
    classpath(deps.spotless.classpath)
  }
}

apply(from = "${project.rootDir}/spotless.gradle.kts")

allprojects {
  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_1_8.toString()
      freeCompilerArgs = freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
  }

  configurations.all {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains.kotlin" && requested.name == "kotlin-reflect") {
        useVersion(versions.kotlin.core)
      }
    }
  }

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

