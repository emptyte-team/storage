import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import net.kyori.indra.IndraExtension
import net.kyori.indra.crossdoc.CrossdocExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.withType
import java.util.Date

class JvmLibraryConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      pluginManager.apply("storage.jvm.publishing")

      pluginManager.apply("net.kyori.indra")
      pluginManager.apply("net.kyori.indra.crossdoc")
      pluginManager.apply("net.kyori.indra.licenser.spotless")

      extensions.configure<SpotlessExtension> {
        fun FormatExtension.applyCommon() {
          trimTrailingWhitespace()
          endWithNewline()
          leadingTabsToSpaces(2)
        }
        java {
          importOrderFile(rootProject.file(".spotless/emptyte.importorder"))
          applyCommon()
        }
        kotlinGradle {
          applyCommon()
        }
      }

      extensions.configure<CrossdocExtension>("indraCrossdoc") {
        baseUrl().set(providers.gradleProperty("javadocPublishRoot"))

        nameBasedDocumentationUrlProvider {
          projectNamePrefix.set("storage-")
        }
      }

      extensions.configure<JavaPluginExtension> {
        withJavadocJar()
      }

      tasks.named<Jar>("jar") {
        manifest {
          attributes(
            "Specification-Version" to project.version,
            "Specification-Vendor" to "emptyte-team",
            "Implementation-Build-Date" to Date()
          )
        }
      }

      tasks.withType<Javadoc>().configureEach {
        options.encoding = Charsets.UTF_8.name()
      }

      tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs.add("-parameters")
        dependsOn("spotlessApply")
      }

      repositories {
        mavenCentral()
      }

      dependencies {
        "compileOnly"(libs.findLibrary("jetbrains.annotations").get())
      }
    }
  }
}
