package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.ComponentValueApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValue
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.lexeme.TextValues
import me.apomazkin.lexeme.Translation

/**
 * IS481 (AGG-2): все API → Domain мапперы lexeme-IS481 в одном файле в `app/`.
 * `app/` видит и `core-db-api`, и `modules/domain/lexeme` — циклов нет.
 *
 * Pass 5 (M13 migration): `ComponentValueData` → `TemplateValues`;
 * `ComponentTypeApiEntity` rename `removeDate → removedAt` + new fields
 * `isMultiple`/`createdAt`/`updatedAt` (синхронно с domain `ComponentType` #7).
 */

fun ComponentTypeApiEntity.toDomain(): ComponentType = ComponentType(
    id = ComponentTypeId(id),
    systemKey = systemKey,
    dictionaryId = dictionaryId,
    name = name,
    template = template,
    position = position,
    isMultiple = isMultiple,
    createdAt = createdAt,
    updatedAt = updatedAt,
    removedAt = removedAt,
)

fun ComponentValueApiEntity.toDomain(): ComponentValue = ComponentValue(
    id = ComponentValueId(id),
    lexemeId = LexemeId(lexemeId),
    type = type.toDomain(),
    data = data,
)

fun QuizConfigApiEntity.toDomain(): QuizConfig = QuizConfig(
    dictionaryId = dictionaryId,
    quizMode = quizMode,
    componentRefs = componentRefs,
)

/**
 * `LexemeApiEntity.toDomain()` — с shim B4/C2:
 * - `translation` ← built-in `BuiltInComponent.TRANSLATION` (AGG-1: только это built-in).
 * - `definition`  ← user-defined `name="Definition", systemKey=null` (AGG-1).
 *
 * Shim ↔ components invariant закрывается в mate refactor (B4/C2 backlog):
 * рассинхрон создаётся **после** маппера через `lexeme.copy(translation = X)`,
 * поэтому debug-check на стороне маппера не приносит пользы — удалён в iter 2.
 */
fun LexemeApiEntity.toDomain(): Lexeme {
    val mapped = components.map { it.toDomain() }
    return Lexeme(
        lexemeId = LexemeId(id),
        components = mapped,
        translation = mapped.toTranslationShim(),
        definition = mapped.toDefinitionShim(),
        addDate = addDate,
        changeDate = changeDate,
    )
}

private fun List<ComponentValue>.toTranslationShim(): Translation? = this
    .firstOrNull { it.type.systemKey == BuiltInComponent.TRANSLATION }
    ?.let { (it.data as? TextValues)?.value?.value }
    ?.let { Translation(it) }

private fun List<ComponentValue>.toDefinitionShim(): Definition? = this
    .firstOrNull { it.type.systemKey == null && it.type.name == "Definition" }
    ?.let { (it.data as? TextValues)?.value?.value }
    ?.let { Definition(it) }
