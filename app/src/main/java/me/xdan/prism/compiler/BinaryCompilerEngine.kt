package me.xdan.prism.compiler

import android.content.Context
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

class BinaryCompilerEngine(private val context: Context) {

    data class CompilationResult(
        val success: Boolean,
        val outputApk: File? = null,
        val error: String? = null
    )

    private val axmlEditor = BinaryXmlEditor()
    private val resTableEditor = ResTableEditor()
    private val zipAligner = ZipAligner()
    private val apkSignerHelper = ApkSignerHelper()
    private val iconPipeline = IconPipeline(context)

    fun compile(
        baseApkAssetPath: String,
        targetPackageName: String,
        targetAppName: String,
        targetUrl: String,
        iconInput: IconPipeline.IconInput,
        iconBackgroundColor: String,
        oldPackageHint: String? = null
    ): CompilationResult {
        try {
            val workingDir = File(context.cacheDir, "build_${System.currentTimeMillis()}")
            workingDir.mkdirs()

            val baseApkFile = File(workingDir, "base.apk")
            context.assets.open(baseApkAssetPath).use { input ->
                FileOutputStream(baseApkFile).use { output ->
                    input.copyTo(output)
                }
            }

            val extractedDir = File(workingDir, "extracted")
            extractedDir.mkdirs()
            ZipFile(baseApkFile).extractAll(extractedDir.absolutePath)

            // 1. Patch AndroidManifest.xml
            val manifestFile = File(extractedDir, "AndroidManifest.xml")
            if (manifestFile.exists()) {
                val manifestBytes = manifestFile.readBytes()
                val patchedManifest = axmlEditor.patchPackageName(manifestBytes, oldPackageHint, targetPackageName)
                manifestFile.writeBytes(patchedManifest)
            }

            // 2. Patch resources.arsc
            val arscFile = File(extractedDir, "resources.arsc")
            if (arscFile.exists()) {
                val arscBytes = arscFile.readBytes()
                val patchedArsc = resTableEditor.patchPackageName(arscBytes, targetPackageName)
                arscFile.writeBytes(patchedArsc)
            }

            // 3. Patch Icons
            val resDir = File(extractedDir, "res")
            iconPipeline.generateIcons(iconInput, iconBackgroundColor, resDir)

            // 4. Re-zip
            val unalignedApk = File(workingDir, "unaligned.apk")
            val zipParameters = ZipParameters()
            zipParameters.compressionMethod = CompressionMethod.DEFLATE
            
            val zipFile = ZipFile(unalignedApk)
            extractedDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    zipFile.addFolder(file, zipParameters)
                } else {
                    // For resources.arsc and some others, they should be STORED
                    if (file.name == "resources.arsc" || file.name.endsWith(".png")) {
                        val storedParams = ZipParameters()
                        storedParams.compressionMethod = CompressionMethod.STORE
                        zipFile.addFile(file, storedParams)
                    } else {
                        zipFile.addFile(file, zipParameters)
                    }
                }
            }

            // 5. ZipAlign
            val alignedApk = File(workingDir, "aligned.apk")
            zipAligner.align(unalignedApk, alignedApk)

            // 6. Sign
            val finalApk = File(context.cacheDir, "prism_${System.currentTimeMillis()}.apk")
            val (keyPair, cert) = apkSignerHelper.generateKeyPairAndCertificate()
            apkSignerHelper.sign(alignedApk, finalApk, keyPair, cert)

            // Cleanup
            workingDir.deleteRecursively()

            return CompilationResult(success = true, outputApk = finalApk)
        } catch (e: Exception) {
            e.printStackTrace()
            return CompilationResult(success = false, error = e.message ?: e.toString())
        }
    }
}
