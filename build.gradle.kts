// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  val kotlinVersion by extra("1.3.21")
  val navVersion by extra("2.0.0-rc02")
  extra.run {
    set("coroutinesVersion", "1.1.1")
    set("lifecycleVersion", "2.0.0")
    set("pagingVersion", "2.1.0")
  }


  repositories {
    google()
    jcenter()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.3.0")
    classpath(kotlin("gradle-plugin", kotlinVersion))
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
  }
}

allprojects {
  repositories {
    google()
    jcenter()
    mavenCentral()
  }
}

tasks.register("clean", Delete::class) {
  delete(rootProject.buildDir)
}

