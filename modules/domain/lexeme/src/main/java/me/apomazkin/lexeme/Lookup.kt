package me.apomazkin.lexeme

/*
 * IS536: ответ сервера слов глазами приложения — домен, которым говорят
 * порты окна снимка и диалога карточки слова. Исходы юзкейсов — тоже
 * домен: по типу на случай, редьюсеры исходы не разбирают. Порядок
 * вариантов sealed — по важности.
 */

/**
 * Токен запроса: [text] как распознан, [confidence] 0…1, [language] по
 * мнению распознавания или язык словаря у карточки слова (LFL-20).
 */
data class LookupToken(
    val text: String,
    val confidence: Float,
    val language: String?,
)

/** Исход распознавания снимка (TFP Ш4–Ш5): токены как распознаны или пусто. */
sealed interface RecognitionOutcome {
    data class Text(
        val tokens: List<LookupToken>,
    ) : RecognitionOutcome

    data object Empty : RecognitionOutcome
}

/**
 * Контекст леммы из ответа `lemmas/from-tokens` (LFL Ш1 А): исправленный
 * [text] снимка и его [translation].
 */
data class LemmaContext(
    val text: String,
    val translation: String,
)

/**
 * Исход запроса значений леммы (LFL Ш8, Д114): слово ответа с леммой и
 * лексемами, словарной формы в базе нет, найдено, но нейронка ничего не
 * выбрала, отказ с причиной, сервер не ответил, слово ответа «ошибка».
 */
sealed interface LexemesOutcome {
    /**
     * [word] — слово ответа; [existing] — леммы словаря снимка с текстом леммы
     * ответа, когда она не та, что просили («children» → «child»), иначе пусто
     * (AWP-43.7): маппер слоя сети словаря не знает, их добавляет юзкейс.
     */
    data class Found(
        val word: LookupWord,
        val recommendations: List<ComponentRecommendation>,
        val existing: List<ExistingLemma> = emptyList(),
    ) : LexemesOutcome

    /** Словарной формы в базе нет ни у слова, ни по связям (LFL-50.2). */
    data object NotFound : LexemesOutcome

    /** `found` с пустыми леммами: нейронка ничего не выбрала (LFL-100.2, AWP-55.7). */
    data object NoLexemes : LexemesOutcome

    data class Denied(
        val reason: LookupDenyReason,
    ) : LexemesOutcome

    /** Сети нет или сервер не ответил (LFL-23). */
    data object Unavailable : LexemesOutcome

    /** Слово ответа `error`: сервер ответил, но по слову не справился (LFL-97). */
    data object WordFailed : LexemesOutcome
}

/** Слово ответа (LFL-50): текст, статус, леммы, замечания по слову. */
data class LookupWord(
    val text: String,
    val status: LookupWordStatus,
    val lemmas: List<LookupLemma>,
    val remarks: List<WordRemark>,
)

/**
 * Лемма ответа (LFL-76): текст, пометка формы, транскрипции записи,
 * лексемы-кандидаты в порядке отбора.
 */
data class LookupLemma(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<LookupLexeme>,
)

/**
 * Лексема-кандидат (LFL-71): данные записи целиком и сопоставление с
 * компонентами словаря (LFL-87); [key] уникален в лемме, [source] и
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
    /** Употреблена в тексте снимка (LFL-113): `true` только у первой лексемы по тапу, без контекста `false`. */
    val inContext: Boolean = false,
)

/** Строка сопоставления: компонент словаря и опция «Выбора» или поле ответа (контракт, 5.1). */
data class ComponentMatch(
    val componentId: Long,
    val optionId: Long?,
    val field: String?,
)

/** Замечание по слову (контракт, 5.6). */
sealed interface WordRemark {
    /** Назначение компонента нейронке непонятно, он оставлен пустым (LFL-88). */
    data class ComponentUnclear(
        val componentId: Long,
    ) : WordRemark

    /**
     * У лексемы-кандидата нет переводов на язык перевода, показано английское
     * толкование (LFL-91).
     */
    data class NoTranslation(
        val lemma: String,
        val lexemeKey: String,
    ) : WordRemark
}

/** Рекомендация завести компонент — ко всему ответу (LFL-89). */
data class ComponentRecommendation(
    val name: String,
    val template: ComponentTemplate,
    val options: List<String>,
)

/** Причина отказа; текст выбирает экран (LFL-63). */
enum class LookupDenyReason {
    NO_ACCESS_KEY,
    UNAUTHORIZED,
    RATE_LIMITED,
    REQUEST_TOO_LARGE,
    UNSUPPORTED_LANGUAGE,

    /** 503: вызовы нейронки были, и ни один не ответил (LFL-63.5). */
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
 * Исход запроса одного слова из исхода запроса по лемме (WCL-04.7, WCL-11):
 * слово ответа `error` — сервер по нему не справился (WCL-11.7); не найдено
 * и «значений нет» — лексем-кандидатов нет; иначе найдено.
 */
fun wordLookupOf(outcome: LexemesOutcome): WordLookupOutcome = when (outcome) {
    is LexemesOutcome.Found -> WordLookupOutcome.Found(
        word = outcome.word,
        recommendations = outcome.recommendations,
    )
    LexemesOutcome.NotFound,
    LexemesOutcome.NoLexemes,
    -> WordLookupOutcome.NotFound
    is LexemesOutcome.Denied -> WordLookupOutcome.Denied(outcome.reason)
    LexemesOutcome.Unavailable -> WordLookupOutcome.Unavailable
    LexemesOutcome.WordFailed -> WordLookupOutcome.WordFailed
}

/**
 * Исход с леммами словаря по лемме ответа (AWP-43.7): у найденного слова
 * лемма ответа с другим текстом, чем просили (ключ текста, AWP-39), —
 * леммы словаря с этим текстом через [load] («children» → «child»); та же
 * лемма или не найдено — исход как есть.
 */
suspend fun LexemesOutcome.withExistingOf(
    requested: String,
    load: suspend (String) -> List<ExistingLemma>,
): LexemesOutcome {
    if (this !is LexemesOutcome.Found) return this
    val answered = word.lemmas.firstOrNull { it.lexemes.isNotEmpty() }?.text ?: return this
    if (wordTextKey(answered) == wordTextKey(requested)) return this
    return copy(existing = load(answered))
}
