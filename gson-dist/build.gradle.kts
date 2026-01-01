plugins {
  alias(libs.plugins.storage.jvm.library)
}

dependencies {
  api(project(":${rootProject.name}-common"))
  api(project(":${rootProject.name}-codec"))

  compileOnlyApi(libs.findLibrary("gson").get())
}
