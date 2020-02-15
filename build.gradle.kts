// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  val kotlinVersion by extra("1.3.61")
  val navVersion by extra("2.3.0-alpha01")
  extra.run {
    set("coroutinesVersion", "1.3.3")
    set("lifecycleVersion", "2.2.0")
    set("pagingVersion", "2.1.1")
    set("koinVersion", "2.0.1")
    set("materialVersion", "1.2.0-alpha04")
    set("glideVersion", "4.11.0")
    set("rxBindingVersion", "3.1.0")
    set("timberVersion", "4.7.1")
    set("rxRelayVersion", "2.1.0")
    set("threetenabpVersion", "1.2.2")
    set("retrofit2Version", "2.7.1")
    set("roomVersion", "2.2.3")
    set("workVersion", "2.3.1")
  }


  repositories {
    google()
    jcenter()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.5.3")
    classpath(kotlin("gradle-plugin", kotlinVersion))
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
    classpath("com.google.gms:google-services:4.3.3")
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
  }
}

tasks.register("clean", Delete::class) { delete(rootProject.buildDir) }

