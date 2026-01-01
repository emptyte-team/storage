plugins {
  alias(libs.plugins.storage.jvm.library)
}

dependencies {
  implementation(project(":${rootProject.name}-common"))
  implementation(project(":${rootProject.name}-codec"))
}
