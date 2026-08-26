package me.xdan.prism.compiler

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.caverock.androidsvg.SVG
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Pipeline for generating adaptive icons for generated WebAPKs.
 */
class IconPipeline(private val context: Context) {

    private val client = OkHttpClient()

    sealed class IconInput {
        data class Url(val url: String) : IconInput()
        data class Svg(val file: File) : IconInput()
        data class Png(val file: File) : IconInput()
    }

    /**
     * Generates all necessary icon resources into the target directory.
     * @param input The source icon (URL, SVG, or PNG)
     * @param backgroundColor Hex color string (e.g., "#FFFFFF")
     * @param targetResDir The extracted res directory of the APK
     */
    fun generateIcons(
        input: IconInput,
        backgroundColor: String,
        targetResDir: File
    ) {
        val colorInt = try {
            Color.parseColor(backgroundColor)
        } catch (e: Exception) {
            Color.WHITE
        }

        val sourceBitmap = when (input) {
            is IconInput.Url -> fetchFavicon(input.url)
            is IconInput.Svg -> renderSvgToBitmap(input.file)
            is IconInput.Png -> BitmapFactory.decodeFile(input.file.absolutePath)
        } ?: return

        // Create directories
        val drawableDir = File(targetResDir, "drawable")
        drawableDir.mkdirs()

        // 1. Generate legacy PNGs for various densities
        generateLegacyIcons(sourceBitmap, targetResDir)

        // 2. Generate foreground layer (PNG)
        val foregroundFile = File(drawableDir, "ic_launcher_foreground.png")
        saveBitmap(sourceBitmap, foregroundFile)

        // 3. Generate adaptive background (XML)
        generateBackground(backgroundColor, targetResDir)

        // 4. Generate monochromatic layer
        // We use a simplified monochromatic version of the source bitmap
        generateMonochrome(sourceBitmap, targetResDir)

        // 5. Generate adaptive XML for API 26+
        generateAdaptiveXml(targetResDir)
    }

    private fun fetchFavicon(url: String): Bitmap? {
        val faviconUrl = if (url.endsWith("/")) "${url}favicon.ico" else "$url/favicon.ico"
        val request = Request.Builder().url(faviconUrl).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun renderSvgToBitmap(file: File): Bitmap? {
        return try {
            file.inputStream().use { inputStream ->
                val svg = SVG.getFromInputStream(inputStream)
                // Adaptive icons should have safe zones, so we might want to scale it
                val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                svg.documentWidth = 512f
                svg.documentHeight = 512f
                svg.renderToCanvas(canvas)
                bitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateLegacyIcons(bitmap: Bitmap, targetResDir: File) {
        val densities = mapOf(
            "mipmap-mdpi" to 48,
            "mipmap-hdpi" to 72,
            "mipmap-xhdpi" to 96,
            "mipmap-xxhdpi" to 144,
            "mipmap-xxxhdpi" to 192
        )

        densities.forEach { (dir, size) ->
            val dirFile = File(targetResDir, dir)
            dirFile.mkdirs()
            val resized = Bitmap.createScaledBitmap(bitmap, size, size, true)
            saveBitmap(resized, File(dirFile, "ic_launcher.png"))
            saveBitmap(resized, File(dirFile, "ic_launcher_round.png"))
        }
    }

    private fun generateBackground(color: String, targetResDir: File) {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <shape xmlns:android="http://schemas.android.com/apk/res/android"
                android:shape="rectangle">
                <solid android:color="$color" />
            </shape>
        """.trimIndent()
        File(targetResDir, "drawable/ic_launcher_background.xml").writeText(xml)
    }

    private fun generateMonochrome(bitmap: Bitmap, targetResDir: File) {
        // Create a monochromatic version by using the alpha channel and making it solid black
        // This allows Android 13+ to apply dynamic color tinting effectively.
        val width = bitmap.width
        val height = bitmap.height
        val monoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(monoBitmap)
        val paint = Paint()
        
        // ColorMatrix to extract alpha and set RGB to black (0,0,0)
        val cm = android.graphics.ColorMatrix(floatArrayOf(
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        saveBitmap(monoBitmap, File(targetResDir, "drawable/ic_launcher_monochrome.png"))
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
        
        val anyDpiDir = File(targetResDir, "mipmap-anydpi-v26")
        anyDpiDir.mkdirs()
        File(anyDpiDir, "ic_launcher.xml").writeText(xml)
        File(anyDpiDir, "ic_launcher_round.xml").writeText(xml)
    }

    private fun saveBitmap(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
