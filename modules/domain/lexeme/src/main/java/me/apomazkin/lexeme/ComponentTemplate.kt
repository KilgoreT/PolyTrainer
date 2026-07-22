package me.apomazkin.lexeme

/**
 * Domain enum шаблонов содержимого компонента. Определяет какой [TemplateValues]
 * variant хранится в `ComponentValue` для конкретного `ComponentType`.
 *
 * M13: drop `LONG_TEXT` (template-key consolidation `long_text → text`); [fromKey]
 * стал nullable (fail-soft парсинг — unknown key логируется в Crashlytics);
 * добавлен computed [fields].
 */
enum class ComponentTemplate(val key: String) {
    TEXT("text"),
    IMAGE("image"),
    CHOICE("choice"),   // IS486: выбор одной опции из набора; значение — ссылка option_id
    // composite — добавляются в будущих фичах
    ;

    val fields: List<Field>
        get() = when (this) {
            TEXT -> listOf(Field("value", PrimitiveType.TEXT))
            IMAGE -> listOf(Field("value", PrimitiveType.IMAGE))
            CHOICE -> emptyList()   // значение выражено ссылкой на опцию, вне fields-модели
        }

    companion object {
        /**
         * Fail-soft парсинг: unknown key → null + caller логирует в Crashlytics
         * (`forward_compat_unknown`).
         */
        fun fromKey(key: String): ComponentTemplate? =
            entries.firstOrNull { it.key == key }
    }
}
