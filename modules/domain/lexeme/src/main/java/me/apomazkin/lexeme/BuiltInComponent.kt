package me.apomazkin.lexeme

/**
 * Domain enum для built-in типов компонентов лексемы.
 *
 * AGG-1: после миграции только `TRANSLATION` остаётся built-in.
 * `definition` мигрирует в user-defined per-dictionary тип `name="Definition"`.
 * Новые built-in значения (pronunciation, transcription, ...) добавляются здесь
 * без миграции схемы.
 */
enum class BuiltInComponent(val key: String) {
    TRANSLATION("translation");

    companion object {
        /** Unknown key → null (user-defined fallback на уровне маппера). */
        fun fromKey(key: String): BuiltInComponent? = entries.firstOrNull { it.key == key }
    }
}
