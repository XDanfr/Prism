package me.xdan.prism.compiler

import android.content.Context
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.io.FileOutputStream

class BinaryCompilerEngine(private val context: Context) {

    data class CompilationResult(
        val success: Boolean,
        val outputApk: File? = null,
        val error: String? = null
    )

    private val axmlEditor = BinaryXmlEditor()
    private val resTableEditor = ResTableEditor()
    private val zipAligner = ZipAligner()
    private val apkSignerHelper = ApkSignerHelper(context)
    private val iconPipeline = IconPipeline(context)

    fun compile(
        baseApkAssetPath: String,
        targetPackageName: String,
        targetAppName: String,
        targetUrl: String,
        iconInput: IconPipeline.IconInput,
        iconBackgroundColor: String,
        configJson: String,
        oldPackageHint: String? = null,
        oldAppNameHint: String = "Prism Web App"
    ): CompilationResult {
        var workingDir: File? = null
        try {
            workingDir = File(context.cacheDir, "build_${System.currentTimeMillis()}").apply { mkdirs() }

            val baseApkFile = File(workingDir, "base.apk")
            context.assets.open(baseApkAssetPath).use { input ->
                FileOutputStream(baseApkFile).use { output -> input.copyTo(output) }
            }

            val extractedDir = File(workingDir, "extracted").apply { mkdirs() }
            ZipFile(baseApkFile).extractAll(extractedDir.absolutePath)

            val manifestFile = File(extractedDir, "AndroidManifest.xml")
            if (manifestFile.exists()) {
                val manifestBytes = manifestFile.readBytes()
                manifestFile.writeBytes(
                    axmlEditor.patchStrings(
                        manifestBytes,
                        mapOf(
                            oldPackageHint.orEmpty() to targetPackageName,
                            oldAppNameHint to targetAppName
                        ).filterKeys(String::isNotBlank)
                    )
                )
            }

            val arscFile = File(extractedDir, "resources.arsc")
            if (arscFile.exists()) {
                arscFile.writeBytes(resTableEditor.patchPackageName(arscFile.readBytes(), targetPackageName))
            }

            File(extractedDir, "assets/prism-config.json").apply {
                parentFile?.mkdirs()
                writeText(configJson)
            }

            iconPipeline.generateIcons(iconInput, iconBackgroundColor, File(extractedDir, "res"))

            val unalignedApk = File(workingDir, "unaligned.apk")
            val zipFile = ZipFile(unalignedApk)
            val zipParameters = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
            }

            extractedDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    zipFile.addFolder(file, zipParameters)
                } else {
                    val storedParams = ZipParameters().apply { compressionMethod = CompressionMethod.STORE }
                    if (file.name == "resources.arsc" || file.name.endsWith(".png")) {
                        zipFile.addFile(file, storedParams)
                    } else {
                        zipFile.addFile(file, zipParameters)
                    }
                }
            }

            val alignedApk = File(workingDir, "aligned.apk")
            zipAligner.align(unalignedApk, alignedApk)

            val finalApk = File(context.cacheDir, "prism_${System.currentTimeMillis()}.apk")
            val signingCredentials = apkSignerHelper.getOrCreateSigningCredentials()
            apkSignerHelper.sign(alignedApk, finalApk, signingCredentials.first, signingCredentials.second)

            return CompilationResult(success = true, outputApk = finalApk)
        } catch (e: Exception) {
            e.printStackTrace()
            return CompilationResult(success = false, error = e.message ?: e.toString())
        } finally {
            workingDir?.deleteRecursively()
        }
    }
}
