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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Распознавание на устройстве (TFP-10). Текст рисуется на Bitmap прямо в
 * тесте — бинарных картинок в репозитории нет. Bundled-модель работает и на
 * эмуляторе CI без Play-сервисов. Адреса строк спеки в именах — `tfp10_k`.
 */
@RunWith(AndroidJUnit4::class)
class MlKitTextRecognizerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val recognizer = MlKitTextRecognizer(context)

    @Test
    fun tfp10_1_drawnHelloWorldGivesTheTwoWordsAndNothingElse() = runBlocking {
        val file = drawToFile(text = "hello world", name = "drawn.jpg")

        val result = recognizer.recognize(Uri.fromFile(file))

        assertEquals(listOf("hello", "world"), result.words.map { it.text.lowercase() })
        assertTrue("lines: ${result.lines}", result.lines.isNotEmpty())
    }

    @Test
    fun tfp10_3_blankImageGivesNoWords() = runBlocking {
        val file = drawToFile(text = null, name = "blank.jpg")

        val result = recognizer.recognize(Uri.fromFile(file))

        assertTrue("words: ${result.words}", result.words.isEmpty())
    }

    /**
     * 12000×12000 — 144 мегапикселя: при декодировании снимок уменьшается до
     * бюджета 12 мегапикселей (в четыре раза по стороне) и читается, памяти
     * хватает. Пиксели лежат в нативной памяти, поэтому полноразмерный битмап
     * здесь рисуется в RGB_565 и сразу отпускается.
     */
    @Test
    fun tfp10_4_hugeImageIsDownscaledAndRecognized() = runBlocking {
        val file = drawToFile(
            text = "hello world",
            name = "huge.jpg",
            width = 12_000,
            height = 12_000,
            textSize = 1_200f,
            config = Bitmap.Config.RGB_565,
        )

        val result = recognizer.recognize(Uri.fromFile(file))

        val words = result.words.map { it.text.lowercase() }
        assertTrue("words: $words", "hello" in words)
        assertTrue("words: $words", "world" in words)
    }

    private fun drawToFile(
        text: String?,
        name: String,
        width: Int = 1200,
        height: Int = 400,
        textSize: Float = 120f,
        config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    ): File {
        val bitmap = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        if (text != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                this.textSize = textSize
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(text, textSize / 2, height / 2f + textSize / 3, paint)
        }
        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return file
    }
}
