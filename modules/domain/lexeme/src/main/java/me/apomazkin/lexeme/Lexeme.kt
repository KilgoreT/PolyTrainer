package me.apomazkin.lexeme

import java.util.Date

@JvmInline
value class LexemeId(val id: Long)

@Deprecated("Use TextValues via components")
@JvmInline
value class Translation(val value: String)

@Deprecated("Use TextValues via components")
@JvmInline
value class Definition(val value: String)

/**
 * Domain entity лексемы.
 *
 * IS481: `components` — source of truth, упорядочен по `ComponentType.position`.
 *
 * `translation` / `definition` — **shim B4/C2**, заполняются маппером
 * `LexemeApiEntity.toDomain()` через lookup в `components`:
 * - `translation` ← built-in `BuiltInComponent.TRANSLATION`.
 * - `definition` ← user-defined `name="Definition", systemKey=null` (AGG-1).
 *
 * Поля @Deprecated и удаляются после mate refactor (backlog).
 */
data class Lexeme(
    val lexemeId: LexemeId,
    val components: List<ComponentValue> = emptyList(),
    @Deprecated("Use components") val translation: Translation? = null,
    @Deprecated("Use components") val definition: Definition? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)
