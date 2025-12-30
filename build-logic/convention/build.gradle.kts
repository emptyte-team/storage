import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
  `kotlin-dsl`
}

group = "team.emptyte.storage.buildlogic"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_21
  }
}

repositories {
  maven(url = "https://repo.stellardrift.ca/repository/internal/") {
    name = "stellardriftReleases"
    mavenContent { releasesOnly() }
  }
  maven(url = "https://repo.stellardrift.ca/repository/snapshots/") {
    name = "stellardriftSnapshots"
    mavenContent { snapshotsOnly() }
  }

  // Use the plugin portal to apply community plugins in convention plugins.
  gradlePluginPortal()
}

dependencies {
  implementation(libs.bundles.indra)

  compileOnly(files(libs::class.java.protectionDomain.codeSource.location))
}

tasks {
  validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
  }
}

gradlePlugin {
  plugins {
    register("jvm-publishing") {
      id = libs.plugins.storage.jvm.publishing.get().pluginId
      implementationClass = "JvmPublishingConventionPlugin"
    }
    register("jvm-library") {
      id = libs.plugins.storage.jvm.library.get().pluginId
      implementationClass = "JvmLibraryConventionPlugin"
    }
  }
}
