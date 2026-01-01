plugins {
  alias(libs.plugins.storage.jvm.library)
}

dependencies {
  api(project(":${rootProject.name}-common"))

  compileOnlyApi(libs.findLibrary("gson").get())
}
