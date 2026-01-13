plugins {
  alias(libs.plugins.storage.jvm.library)
}

dependencies {
  api(project(":${rootProject.name}-api"))
  api(project(":${rootProject.name}-codec"))

  compileOnlyApi(libs.findLibrary("configurate.yaml").get())
}
