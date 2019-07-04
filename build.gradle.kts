// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
  val kotlinVersion by extra("1.3.40")
  val navVersion by extra("2.1.0-alpha06")
  extra.run {
    set("coroutinesVersion", "1.3.0-M2")
    set("lifecycleVersion", "2.2.0-alpha01")
    set("pagingVersion", "2.1.0")
    set("koinVersion", "2.0.0")
    set("materialVersion", "1.1.0-alpha06")
    set("glideVersion", "4.9.0")
    set("rxBindingVersion", "3.0.0-alpha2")
    set("timberVersion", "4.7.1")
    set("rxRelayVersion", "2.1.0")
    set("threetenabpVersion", "1.1.2")
    set("retrofit2Version", "2.6.0")
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
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
  }
}

tasks.register("clean", Delete::class) { delete(rootProject.buildDir) }

