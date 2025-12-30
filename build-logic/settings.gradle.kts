pluginManagement {
  repositories {
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }

  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

rootProject.name = "storage-build-logic"

include(":${rootProject.name}-convention")
project(":${rootProject.name}-convention").projectDir = file("convention")
