plugins {
  alias(libs.plugins.storage.jvm.library)
}

dependencies {
  compileOnly(project(":${rootProject.name}-common"))
}
