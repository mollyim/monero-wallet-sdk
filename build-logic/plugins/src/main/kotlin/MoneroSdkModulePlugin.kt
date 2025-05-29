import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.com.google.gson.Gson
import java.io.File

class MoneroSdkModulePlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        apply(plugin = "jacoco")
        configureJacoco(extensions.getByType<LibraryAndroidComponentsExtension>())

        tasks.register("version") {
            doLast {
                val versionInfo = mapOf(
                    "version" to version,
                    "isSnapshot" to isSnapshot,
                )

                println(Gson().toJson(versionInfo))
            }
        }
    }
}

val Project.vendorDir: File
    get() = rootProject.layout.projectDirectory.dir("vendor").asFile

val Project.downloadCacheDir: File
    get() = layout.buildDirectory.dir("downloads").get().asFile

val Project.isSnapshot: Boolean
    get() = version.toString().endsWith("SNAPSHOT")
