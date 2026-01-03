import net.kyori.indra.IndraExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class JvmPublishingConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      apply(plugin = "net.kyori.indra.publishing")

      extensions.configure<IndraExtension> {
        javaVersions {
          target(21)
          minimumToolchain(21)
          strictVersions()
        }

        github("emptyte-team", "storage") {
          ci(true)
        }
        mitLicense()
      }
    }
  }
}
