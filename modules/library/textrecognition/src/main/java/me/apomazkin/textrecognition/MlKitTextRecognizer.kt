package me.apomazkin.textrecognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import com.google.mlkit.vision.text.TextRecognizer as MlKitClient

private const val DEFAULT_MAX_PIXELS = 12_000_000
private const val UNDETERMINED_LANGUAGE = "und"

/**
 * Латиница моделью ML Kit, встроенной в APK. Картинка уменьшается ещё при
 * декодировании до бюджета [maxPixels]: скриншоты и фото до 12 Мп идут
 * как есть, фото 48 Мп сжимается вчетверо (полноразмерное — сотни
 * мегабайт в памяти). Поворот берётся из EXIF — без него снимок с камеры
 * уходит в модель «боком» и читается мусором. Недекодируемая картинка —
 * исключение: юзер должен увидеть ошибку, а не «Слов не найдено».
 */
class MlKitTextRecognizer(
    private val context: Context,
    private val maxPixels: Int = DEFAULT_MAX_PIXELS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : TextRecognizer {

    private val client: MlKitClient by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun recognize(uri: Uri): RecognizedText = withContext(dispatcher) {
        val bitmap = decodeScaled(uri) ?: throw IOException("Cannot decode image $uri")
        val image = InputImage.fromBitmap(bitmap, readRotation(uri))
        // recycle() только после завершения задачи: await() при отмене
        // бросает, а задача ML Kit продолжает читать битмап.
        val text = client.process(image).await()
        bitmap.recycle()
        text.toRecognized()
    }

    private fun decodeScaled(uri: Uri): Bitmap? {
        // decodeStream с inJustDecodeBounds всегда возвращает null — размеры
        // читаются из options, поэтому проверка на null только у потока.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = openStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val pixels = bounds.outWidth.toLong() * bounds.outHeight
        var sampleSize = 1
        while (pixels / (sampleSize.toLong() * sampleSize) > maxPixels) sampleSize *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val stream = openStream(uri) ?: return null
        return stream.use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Градусы 0/90/180/270; у PNG без EXIF — 0. Зеркальные варианты учтены библиотекой. */
    private fun readRotation(uri: Uri): Int {
        val stream = openStream(uri) ?: return 0
        return stream.use { ExifInterface(it).rotationDegrees }
    }

    private fun openStream(uri: Uri) = context.contentResolver.openInputStream(uri)

    private fun Text.toRecognized(): RecognizedText = RecognizedText(
        lines = textBlocks.flatMap { block -> block.lines.map { it.text } },
        words = textBlocks.flatMap { block ->
            block.lines.flatMap { line ->
                line.elements.map { element ->
                    val language = element.recognizedLanguage
                    RecognizedWord(
                        text = element.text,
                        confidence = element.confidence,
                        language = language.takeUnless { it == UNDETERMINED_LANGUAGE },
                    )
                }
            }
        },
    )
}
