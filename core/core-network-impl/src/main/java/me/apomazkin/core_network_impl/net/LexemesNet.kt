package me.apomazkin.core_network_impl.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Формат провода `lexemesFromLemma` один в один с `backend/api/openapi.yaml`
 * 0.5.0 (контракт, разделы 1 и 2). Дерево компонентов, слово ответа, леммы,
 * лексемы и рекомендации — общие классы `WordNet.kt`. Наружу слоя классы не
 * выходят: маппинг в зеркала api-модуля — `mapper/LexemesMappers.kt`.
 */

/** Тело запроса: `context` null — поля нет (контракт 1.1), `components` null — поля нет (LFL-78.7). */
@Serializable
internal data class LexemesRequestNet(
    @SerialName("learningLanguage") val learningLanguage: String,
    @SerialName("translationLanguage") val translationLanguage: String,
    @SerialName("lemma") val lemma: String,
    @SerialName("context") val context: LemmaContextNet? = null,
    @SerialName("components") val components: List<ComponentNet>? = null,
)

/** Текст снимка и его перевод; перевода нет — null (контракт 1.2). */
@Serializable
internal data class LemmaContextNet(
    @SerialName("text") val text: String,
    @SerialName("translation") val translation: String?,
)

/** Ответ одним словом; рекомендаций нет — пусто. */
@Serializable
internal data class LexemesResponseNet(
    @SerialName("attribution") val attribution: String,
    @SerialName("word") val word: WordResultNet,
    @SerialName("recommendations") val recommendations: List<ComponentRecommendationNet> = emptyList(),
)
