package com.hoc.comicapp.plugin

import appConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import versions
import java.lang.System.getenv
import java.util.Properties

private inline val Project.libraryExtension get() = extensions.getByType<LibraryExtension>()
private inline val Project.appExtension get() = extensions.getByType<AppExtension>()
private inline val Project.javaPluginExtension get() = extensions.getByType<JavaPluginExtension>()

open class ComicAppExtension {
  var viewBinding: Boolean = false
  var parcelize: Boolean = false
}

class ComicAppPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.run {
      println("Setup $this")
      plugins.all {
        when (this) {
          is JavaPlugin, is JavaLibraryPlugin -> {
            println(" >>> is JavaPlugin, is JavaLibraryPlugin")
            javaPluginExtension.run {
              targetCompatibility = VERSION_1_8
              sourceCompatibility = VERSION_1_8
            }
          }
          is LibraryPlugin -> {
            println(" >>> is LibraryPlugin")
            plugins.apply("kotlin-android")
            configAndroidLibrary()
          }
          is AppPlugin -> {
            println(" >>> is AppPlugin")
            plugins.apply("kotlin-android")
            configAndroidApplication()
          }
          is KotlinBasePluginWrapper -> {
            println(" >>> is KotlinBasePluginWrapper")
            configKotlinOptions()
          }
        }
      }
    }

    val comicAppExtension = project.extensions.create("comicApp", ComicAppExtension::class)

    project.afterEvaluate {
      project.plugins.all {
        when (this) {
          is LibraryPlugin -> {
            libraryExtension.buildFeatures {
              viewBinding = comicAppExtension.viewBinding
              dataBinding = false
            }
            enableParcelize(comicAppExtension.parcelize)
          }
          is AppPlugin -> {
            appExtension.buildFeatures.run {
              viewBinding = comicAppExtension.viewBinding
            }
            enableParcelize(comicAppExtension.parcelize)
          }
        }
      }
    }
  }
}

private fun Project.enableParcelize(enabled: Boolean) {
  if (enabled) {
    plugins.apply("kotlin-parcelize")
  }
}

private fun Project.configKotlinOptions() {
  tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
      val version = VERSION_1_8.toString()
      jvmTarget = version
      sourceCompatibility = version
      targetCompatibility = version
      freeCompilerArgs = freeCompilerArgs + listOf("-opt-in=kotlin.RequiresOptIn")
      languageVersion = "1.6"
    }
  }
}

private fun Project.configAndroidLibrary() = libraryExtension.run {
  compileSdk = versions.sdk.compile
  buildToolsVersion = versions.sdk.buildTools

  defaultConfig {
    minSdk = versions.sdk.min
    targetSdk = versions.sdk.target

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
  }
}

private fun Project.configAndroidApplication() = appExtension.run {
  buildToolsVersion(versions.sdk.buildTools)
  compileSdkVersion(versions.sdk.compile)

  defaultConfig {
    applicationId = appConfig.applicationId

    minSdk = versions.sdk.min
    targetSdk = versions.sdk.target

    versionCode = appConfig.versionCode
    versionName = appConfig.versionName

    resourceConfigurations.addAll(appConfig.supportedLocales)

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    getByName("debug") {
      val keystoreProperties = Properties().apply {
        load(
          rootProject.file("keystore/debugkey.properties")
            .apply { check(exists()) }
            .reader()
        )
      }

      keyAlias = keystoreProperties["keyAlias"] as String
      keyPassword = keystoreProperties["keyPassword"] as String
      storeFile = rootProject.file(keystoreProperties["storeFile"] as String).apply { check(exists()) }
      storePassword = keystoreProperties["storePassword"] as String
    }

    create("release") {
      val keystoreProperties by lazy {
        Properties().apply {
          load(
            rootProject.file("keystore/debugkey.properties")
              .apply { check(exists()) }
              .reader()
          )
        }
      }

      keyAlias = getenv("keyAlias") ?: keystoreProperties["keyAlias"] as String
      keyPassword = getenv("keyPassword") ?: keystoreProperties["keyPassword"] as String
      storeFile = (getenv("storeFile")?.let { rootProject.file(it) }
        ?: rootProject.file(keystoreProperties["storeFile"] as String)).apply { check(exists()) }
      storePassword = getenv("storePassword") ?: keystoreProperties["storePassword"] as String

      // Optional, specify signing versions used
      enableV1Signing = true
      enableV2Signing = true
    }
  }

  buildTypes {
    getByName("debug") {
      isMinifyEnabled = false
      isShrinkResources = false

      // proguardFiles(
      //   getDefaultProguardFile("proguard-android-optimize.txt"),
      //   "proguard-rules.pro"
      // )

      signingConfig = signingConfigs.getByName("debug")
      isDebuggable = true
    }

    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      signingConfig = signingConfigs.getByName("release")
      isDebuggable = false
    }
  }

  compileOptions {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
  }
}
