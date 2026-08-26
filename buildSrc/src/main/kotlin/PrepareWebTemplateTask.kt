import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class PrepareWebTemplateTask : DefaultTask() {
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    // This directory is produced by :webtemplate:assembleRelease, which is an
    // explicit task dependency. It must not be validated as a pre-existing
    // input, otherwise Gradle can reject the task before that dependency runs.
    @get:Internal
    abstract val inputDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyTemplate() {
        fileSystemOperations.sync {
            from(inputDirectory)
            include("*.apk")
            into(outputDirectory.dir("generated"))
            rename { "base-release.apk" }
            includeEmptyDirs = false
        }
    }
}
