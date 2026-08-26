package me.xdan.prism.compiler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.caverock.androidsvg.SVG
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/** Generates icon resources for generated Prism WebAPKs. */
class IconPipeline(private val context: Context) {
    private val client = OkHttpClient()

    sealed class IconInput {
        data class Url(val url: String) : IconInput()
        data class Svg(val file: File) : IconInput()
        data class Png(val file: File) : IconInput()
    }

    fun generateIcons(input: IconInput, backgroundColor: String, targetResDir: File) {
        val sourceBitmap = when (input) {
            is IconInput.Url -> fetchFavicon(input.url)
            is IconInput.Svg -> renderSvgToBitmap(input.file)
            is IconInput.Png -> BitmapFactory.decodeFile(input.file.absolutePath)
        } ?: generateGlobeBitmap()

        targetResDir.mkdirs()
        val drawableDir = File(targetResDir, "drawable").apply { mkdirs() }

        generateLegacyIcons(sourceBitmap, targetResDir)
        saveBitmap(sourceBitmap, File(drawableDir, "ic_launcher.png"))
        saveBitmap(sourceBitmap, File(drawableDir, "ic_launcher_foreground.png"))
        generateBackground(backgroundColor, targetResDir)
        generateMonochrome(sourceBitmap, targetResDir)
        generateAdaptiveXml(targetResDir)
    }

    private fun fetchFavicon(url: String): Bitmap? {
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val faviconUrl = "${uri.scheme ?: "https"}://$host/favicon.ico"
        val request = Request.Builder().url(faviconUrl).build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.byteStream()?.use(BitmapFactory::decodeStream) else null
            }
        }.getOrNull()
    }

    private fun renderSvgToBitmap(file: File): Bitmap? = runCatching {
        file.inputStream().use { inputStream ->
            val svg = SVG.getFromInputStream(inputStream)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            svg.documentWidth = 512f
            svg.documentHeight = 512f
            svg.renderToCanvas(canvas)
            bitmap
        }
    }.getOrNull()

    private fun generateLegacyIcons(bitmap: Bitmap, targetResDir: File) {
        val densities = mapOf(
            "mipmap-mdpi" to 48,
            "mipmap-hdpi" to 72,
            "mipmap-xhdpi" to 96,
            "mipmap-xxhdpi" to 144,
            "mipmap-xxxhdpi" to 192
        )
        densities.forEach { (dir, size) ->
            val dirFile = File(targetResDir, dir).apply { mkdirs() }
            val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
            saveBitmap(resized, File(dirFile, "ic_launcher.png"))
            saveBitmap(resized, File(dirFile, "ic_launcher_round.png"))
        }
    }

    private fun generateBackground(color: String, targetResDir: File) {
        val safeColor = runCatching { Color.parseColor(color) }.getOrElse { Color.DKGRAY }
        val normalized = String.format("#%08X", safeColor)
        File(targetResDir, "drawable/ic_launcher_background.xml").apply {
            parentFile?.mkdirs()
            writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
                    <solid android:color="$normalized" />
                </shape>
                """.trimIndent()
            )
        }
    }

    private fun generateMonochrome(bitmap: Bitmap, targetResDir: File) {
        val mono = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mono)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(
                android.graphics.ColorMatrix(floatArrayOf(
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                ))
            )
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        saveBitmap(mono, File(targetResDir, "drawable/ic_launcher_monochrome.png"))
    }

    private fun generateAdaptiveXml(targetResDir: File) {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@drawable/ic_launcher_background" />
                <foreground android:drawable="@drawable/ic_launcher_foreground" />
                <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
            </adaptive-icon>
        """.trimIndent()
        val anyDpiDir = File(targetResDir, "mipmap-anydpi-v26").apply { mkdirs() }
        File(anyDpiDir, "ic_launcher.xml").writeText(xml)
        File(anyDpiDir, "ic_launcher_round.xml").writeText(xml)
    }

    private fun generateGlobeBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 26f
        }
        canvas.drawCircle(256f, 256f, 190f, paint)
        canvas.drawOval(175f, 66f, 337f, 446f, paint)
        canvas.drawOval(66f, 175f, 446f, 337f, paint)
        canvas.drawLine(66f, 256f, 446f, 256f, paint)
        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
