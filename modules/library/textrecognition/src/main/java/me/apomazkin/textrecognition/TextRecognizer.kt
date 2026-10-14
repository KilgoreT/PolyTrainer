package me.apomazkin.textrecognition

import android.net.Uri

/**
 * Слово из распознанного текста: [confidence] 0…1 (в unbundled-варианте
 * ML Kit всегда 0), [language] — код BCP-47 или null, если модель язык не
 * определила.
 */
data class RecognizedWord(
    val text: String,
    val confidence: Float,
    val language: String?,
)

/** Текст с картинки: строки — для сверки с фото в логах, слова — для списка. */
data class RecognizedText(
    val lines: List<String>,
    val words: List<RecognizedWord>,
) {
    companion object {
        val Empty = RecognizedText(lines = emptyList(), words = emptyList())
    }
}

interface TextRecognizer {
    suspend fun recognize(uri: Uri): RecognizedText
}
