package me.apomazkin.candidates

import androidx.compose.runtime.Immutable
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.LookupWordStatus

/*
 * IS536: ответ сервера глазами экранов — UI-двойники домена
 * `LookupWord` и компании, поле в поле (`mapper/LookupMappers.kt`).
 * Домен в редьюсер и composable не попадает.
 */

/** Слово ответа (LFL-50): текст, статус, леммы, замечания по слову. */
@Immutable
data class LookupWordUi(
    val text: String,
    val status: LookupWordStatus,
    val lemmas: List<LookupLemmaUi>,
    val remarks: List<WordRemarkUi>,
)

/** Лемма ответа (LFL-76): текст, пометка формы, транскрипции, лексемы-кандидаты в порядке отбора. */
@Immutable
data class LookupLemmaUi(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<LookupLexemeUi>,
)

/** Лексема-кандидат (LFL-71): данные записи целиком и сопоставление с компонентами (LFL-87). */
@Immutable
data class LookupLexemeUi(
    val key: String,
    val partOfSpeechKey: String?,
    val source: String,
    val translations: List<String>,
    val gloss: String?,
    val caption: String?,
    val notes: List<String>,
    val examples: List<String>,
    val grammar: Map<String, String>,
    val level: String?,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val componentMatches: List<ComponentMatchUi>,
    /** Пометка «в тексте» от сервера (LFL-113): без контекста всегда `false`. */
    val inContext: Boolean = false,
)

/** Строка сопоставления: компонент и опция «Выбора» или поле ответа. */
@Immutable
data class ComponentMatchUi(
    val componentId: Long,
    val optionId: Long?,
    val field: String?,
)

/** Замечание по слову (контракт, 5.6). */
sealed interface WordRemarkUi {
    data class ComponentUnclear(
        val componentId: Long,
    ) : WordRemarkUi

    data class NoTranslation(
        val lemma: String,
        val lexemeKey: String,
    ) : WordRemarkUi
}

/** Рекомендация завести компонент — ко всему ответу (LFL-89). */
@Immutable
data class ComponentRecommendationUi(
    val name: String,
    val template: ComponentTemplate,
    val options: List<String>,
)
