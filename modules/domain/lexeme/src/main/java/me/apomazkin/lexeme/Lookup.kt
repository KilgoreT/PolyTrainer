package me.apomazkin.lexeme

/*
 * IS536: ответ сервера слов глазами приложения — домен, которым говорят
 * порты окна снимка и диалога карточки слова. Исходы юзкейсов — тоже
 * домен: по типу на случай, редьюсеры исходы не разбирают. Порядок
 * вариантов sealed — по важности.
 */

/**
 * Токен запроса: [text] как распознан, [confidence] 0…1, [language] по
 * мнению распознавания или язык словаря у карточки слова (WFT-20).
 */
data class LookupToken(
    val text: String,
    val confidence: Float,
    val language: String?,
)

/** Исход распознавания снимка (WFP Ш4–Ш5): токены как распознаны или пусто. */
sealed interface RecognitionOutcome {
    data class Text(
        val tokens: List<LookupToken>,
    ) : RecognitionOutcome

    data object Empty : RecognitionOutcome
}

/**
 * Исход запроса слов (WFT Ш1…Ш8): слова есть, ответ без слов, отказ с
 * причиной, сервер не ответил.
 */
sealed interface LookupOutcome {
    /** [dictionaryId] — словарь снимка (AWP-01), [words] в порядке ответа, [recommendations] ко всему ответу. */
    data class Words(
        val dictionaryId: Long,
        val words: List<LookupWord>,
        val recommendations: List<ComponentRecommendation>,
    ) : LookupOutcome

    data object NoWords : LookupOutcome

    data class Denied(
        val reason: LookupDenyReason,
    ) : LookupOutcome

    data object Unavailable : LookupOutcome
}

/** Слово ответа (WFT-50): текст, статус, леммы, замечания по слову. */
data class LookupWord(
    val text: String,
    val status: LookupWordStatus,
    val lemmas: List<LookupLemma>,
    val remarks: List<WordRemark>,
)

/** Лемма ответа (WFT-76): текст, пометка формы, транскрипции записи, лексемы-кандидаты в порядке отбора. */
data class LookupLemma(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<LookupLexeme>,
)

/**
 * Лексема-кандидат (WFT-71): данные записи целиком и сопоставление с
 * компонентами словаря (WFT-87); [key] уникален в лемме, [source] и
 * [partOfSpeechKey] — строки как у сервера.
 */
data class LookupLexeme(
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
    val componentMatches: List<ComponentMatch>,
)

/** Строка сопоставления: компонент словаря и опция «Выбора» или поле ответа (контракт, 5.1). */
data class ComponentMatch(
    val componentId: Long,
    val optionId: Long?,
    val field: String?,
)

/** Замечание по слову (контракт, 5.6). */
sealed interface WordRemark {
    /** Назначение компонента нейронке непонятно, он оставлен пустым (WFT-88). */
    data class ComponentUnclear(
        val componentId: Long,
    ) : WordRemark

    /** У лексемы-кандидата нет переводов на язык перевода, показано английское толкование (WFT-91). */
    data class NoTranslation(
        val lemma: String,
        val lexemeKey: String,
    ) : WordRemark
}

/** Рекомендация завести компонент — ко всему ответу (WFT-89). */
data class ComponentRecommendation(
    val name: String,
    val template: ComponentTemplate,
    val options: List<String>,
)

/** Причина отказа; текст выбирает экран (WFT-63). */
enum class LookupDenyReason {
    NO_ACCESS_KEY,
    UNAUTHORIZED,
    RATE_LIMITED,
    REQUEST_TOO_LARGE,
    UNSUPPORTED_LANGUAGE,

    /** 503: вызовы нейронки были, и ни один не ответил (WFT-63.5). */
    LLM_UNAVAILABLE,
}

enum class LookupWordStatus {
    FOUND,
    NOT_FOUND,
    ERROR,
}

/**
 * Исход запроса по слову карточки (WCL Ш1): найдено, ни одной
 * лексемы-кандидата, слово удалено, отказ, сервер не ответил, сервер по
 * слову не справился.
 */
sealed interface WordLookupOutcome {
    data class Found(
        val word: LookupWord,
        val recommendations: List<ComponentRecommendation>,
    ) : WordLookupOutcome

    data object NotFound : WordLookupOutcome

    data object WordMissing : WordLookupOutcome

    data class Denied(
        val reason: LookupDenyReason,
    ) : WordLookupOutcome

    /** Сервер не ответил (WCL-11.3). */
    data object Unavailable : WordLookupOutcome

    /** Слово ответа `error`: сервер ответил, но по слову не справился (WCL-11.7). */
    data object WordFailed : WordLookupOutcome
}

/**
 * Исход запроса одного слова из исхода запроса (WCL-04.7, WCL-11): слово
 * ответа `error` — сервер по нему не справился (WCL-11.7); `not_found`,
 * пустой ответ или леммы без лексем — лексем-кандидатов нет; иначе
 * найдено.
 */
fun wordLookupOf(outcome: LookupOutcome): WordLookupOutcome = when (outcome) {
    is LookupOutcome.Words -> {
        val word = outcome.words.firstOrNull()
        when {
            word == null -> WordLookupOutcome.NotFound
            word.status == LookupWordStatus.ERROR -> WordLookupOutcome.WordFailed
            word.lemmas.none { it.lexemes.isNotEmpty() } -> WordLookupOutcome.NotFound
            else -> WordLookupOutcome.Found(word = word, recommendations = outcome.recommendations)
        }
    }
    LookupOutcome.NoWords -> WordLookupOutcome.NotFound
    is LookupOutcome.Denied -> WordLookupOutcome.Denied(outcome.reason)
    LookupOutcome.Unavailable -> WordLookupOutcome.Unavailable
}
