package me.apomazkin.core_network_api.entity

/**
 * Запрос `lookupWords` (WFT-20): токены как распознаны, без нормализации
 * (её делает сервер), языки словаря в BCP-47, дерево компонентов словаря
 * (WFT-78); пустое дерево — поля в запросе нет (WFT-78.7).
 */
data class LookupRequestApiEntity(
    val learningLanguage: String,
    val translationLanguage: String,
    val tokens: List<TokenApiEntity>,
    val components: List<ComponentNodeApiEntity>,
)

/**
 * Токен для сервера: [text] как распознан, [confidence] 0…1, [language] по
 * мнению распознавания или null (WFT-20.2).
 */
data class TokenApiEntity(
    val text: String,
    val confidence: Float,
    val language: String?,
)

/**
 * Узел дерева компонентов, как `Component` в `openapi.yaml`: [key] у
 * встроенного компонента, null у своего; [template] — ключ шаблона;
 * [options] — опции «Выбора»; [children] — компоненты, зависящие от того,
 * что у этого есть значение.
 */
data class ComponentNodeApiEntity(
    val id: Long,
    val key: String?,
    val name: String,
    val template: String,
    val core: Boolean,
    val multiple: Boolean,
    val options: List<ComponentOptionNodeApiEntity>,
    val children: List<ComponentNodeApiEntity>,
)

/** Опция «Выбора» в дереве: [key] встроенной опции, [children] — компоненты, зависящие от её выбора. */
data class ComponentOptionNodeApiEntity(
    val id: Long,
    val key: String?,
    val label: String,
    val children: List<ComponentNodeApiEntity>,
)
