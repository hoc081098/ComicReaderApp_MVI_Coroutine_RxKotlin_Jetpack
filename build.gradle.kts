// Top-level build file where you can add configuration options common to all sub-projects/modules.

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

subprojects {
  apply(from = "${rootProject.rootDir}/spotless.gradle.kts")
}

allprojects {
  apply(plugin = "com.github.ben-manes.versions")

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = JavaVersion.VERSION_1_8.toString()
      freeCompilerArgs += listOf("-Xopt-in=kotlin.RequiresOptIn")
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
  }
}

tasks.register("clean", Delete::class) { delete(rootProject.buildDir) }

