package me.apomazkin.core_network_impl.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Формат провода `lemmasFromTokens` один в один с `backend/api/openapi.yaml`
 * 0.4.0. Наружу слоя эти классы не выходят: маппинг в зеркала api-модуля —
 * `mapper/LemmasMappers.kt`. Токен ([TokenNet]) — в `WordNet.kt`.
 */

@Serializable
internal data class LemmasRequestNet(
    @SerialName("learningLanguage") val learningLanguage: String,
    @SerialName("translationLanguage") val translationLanguage: String,
    @SerialName("tokens") val tokens: List<TokenNet>,
)

/** Тело 200 (LFT-14); списки с умолчаниями: разбор не падает, если поле не пришло. */
@Serializable
internal data class LemmasResponseNet(
    @SerialName("text") val text: String,
    @SerialName("translation") val translation: String,
    @SerialName("lexical") val lexical: List<String> = emptyList(),
    @SerialName("proper") val proper: List<String> = emptyList(),
    @SerialName("function") val function: List<String> = emptyList(),
    @SerialName("unparsed") val unparsed: List<String> = emptyList(),
)
