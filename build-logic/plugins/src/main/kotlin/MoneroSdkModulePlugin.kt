import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import java.io.File

class MoneroSdkModulePlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        apply(plugin = "jacoco")
        configureJacoco(extensions.getByType<LibraryAndroidComponentsExtension>())
    }
}

val Project.vendorDir: File
    get() = rootProject.layout.projectDirectory.dir("vendor").asFile

val Project.downloadCacheDir: File
    get() = layout.buildDirectory.dir("downloads").get().asFile
