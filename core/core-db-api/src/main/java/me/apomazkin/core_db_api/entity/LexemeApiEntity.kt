package me.apomazkin.core_db_api.entity

import java.util.Date

/**
 * IS481: value-классы оставлены для @Deprecated overload'ов
 * `addLexemeWithTranslation` / `updateLexemeTranslation` (A3 shim).
 * `DefinitionApiEntity` оставлен временно для compatibility — больше нигде
 * не используется в LexemeApi (AGG-6: definition wrappers удалены).
 */
@JvmInline
value class TranslationApiEntity(val value: String)

@JvmInline
value class DefinitionApiEntity(val value: String)

/**
 * IS481 (AGG-6): `translation` / `definition` поля удалены — единственный
 * источник истины — `components`. Domain `Lexeme` имеет shim-поля,
 * заполняемые маппером `LexemeApiEntity.toDomain()` через lookup в `components`.
 */
data class LexemeApiEntity(
    val id: Long,
    val components: List<ComponentValueApiEntity> = emptyList(),
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
)

// canRemoveTranslation / canRemoveDefinition удалены (AGG-6) —
// callsite WordCardUseCaseImpl переписан на generic deleteComponentValue
// → RemoveComponentResult.
