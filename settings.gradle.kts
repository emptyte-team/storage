pluginManagement {
  includeBuild("build-logic")

  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "storage"

sequenceOf(
  "api",
  "codec",
).forEach {
  include("${rootProject.name}-$it")
  project(":${rootProject.name}-$it").projectDir = file(it)
}

sequenceOf(
  "gson",
  "yaml",
  "caffeine"
).forEach {
  include("${rootProject.name}-$it-dist")
  project(":${rootProject.name}-$it-dist").projectDir = file("$it-dist")
}
