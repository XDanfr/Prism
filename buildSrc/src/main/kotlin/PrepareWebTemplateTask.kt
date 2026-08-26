import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class PrepareWebTemplateTask : DefaultTask() {
    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    // The producing task (:webtemplate:assembleRelease) runs first. Mark this
    // as internal so Gradle does not validate the file before that dependency
    // has had a chance to create it.
    @get:Internal
    abstract val inputApk: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyTemplate() {
        fileSystemOperations.sync {
            from(inputApk)
            into(outputDirectory)
            rename { "base-release.apk" }
        }
    }
}
