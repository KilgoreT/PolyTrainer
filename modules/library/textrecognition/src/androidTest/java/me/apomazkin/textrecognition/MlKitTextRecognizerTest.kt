package me.apomazkin.textrecognition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Текст рисуется на Bitmap прямо в тесте — бинарных картинок в репозитории
 * нет. Bundled-модель работает и на эмуляторе CI без Play-сервисов.
 */
@RunWith(AndroidJUnit4::class)
class MlKitTextRecognizerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recognizer = MlKitTextRecognizer(context)

    @Test
    fun drawnLatinTextIsRecognizedAsWords() = runBlocking {
        val file = drawToFile(text = "hello world", name = "drawn.jpg")

        val result = recognizer.recognize(Uri.fromFile(file))

        val words = result.words.map { it.text.lowercase() }
        assertTrue("words: $words", "hello" in words)
        assertTrue("words: $words", "world" in words)
        assertTrue("lines: ${result.lines}", result.lines.isNotEmpty())
    }

    @Test
    fun blankImageGivesNoWords() = runBlocking {
        val file = drawToFile(text = null, name = "blank.jpg")

        val result = recognizer.recognize(Uri.fromFile(file))

        assertTrue("words: ${result.words}", result.words.isEmpty())
    }

    private fun drawToFile(text: String?, name: String): File {
        val bitmap = Bitmap.createBitmap(1200, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        if (text != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 120f
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(text, 60f, 240f, paint)
        }
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return file
    }
}
