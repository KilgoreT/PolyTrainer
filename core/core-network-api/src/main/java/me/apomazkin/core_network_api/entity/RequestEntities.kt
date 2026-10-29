package me.apomazkin.core_network_api.entity

/**
 * Токен для сервера (LFT-02): [text] как распознан, без нормализации (её
 * делает сервер), [confidence] 0…1, [language] по мнению распознавания или
 * null.
 */
data class TokenApiEntity(
    val text: String,
    val confidence: Float,
    val language: String?,
)

/**
 * Узел дерева компонентов, как `Component` в `openapi.yaml` (LFL-78): [key] у
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
