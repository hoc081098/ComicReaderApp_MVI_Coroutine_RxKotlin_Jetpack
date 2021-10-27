include(":navigation")
include(":utils")
include(":base-ui")
include(":app")
includeProject(":feature-home", "features/feature-home")

fun includeProject(name: String, filePath: String) {
  include(name)
  project(name).projectDir = File(filePath)
}