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
  "gson-dist",
  "caffeine-dist"
).forEach {
  include("${rootProject.name}-$it")
  project(":${rootProject.name}-$it").projectDir = file(it)
}
