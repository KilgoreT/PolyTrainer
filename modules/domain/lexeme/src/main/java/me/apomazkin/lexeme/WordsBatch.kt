package me.apomazkin.lexeme

/**
 * Пакет слов с лексемами для записи одной транзакцией (AWP-20): слова
 * новые или существующие, у каждого своя группа или без неё.
 */
data class WordsBatch(
    val dictionaryId: Long,
    val words: List<BatchWord>,
)

/**
 * Слово пакета. [wordId] — существующее слово словаря, null — новое с
 * текстом [text]; [groupId] — живая группа словаря, null — без членства.
 */
data class BatchWord(
    val wordId: Long?,
    val text: String,
    val groupId: Long?,
    val lexemes: List<BatchLexeme>,
)

/** Лексема пакета: значения компонентов по id типов словаря. */
data class BatchLexeme(
    val components: List<BatchComponent>,
)

data class BatchComponent(
    val typeId: Long,
    val data: TemplateValues,
)

/**
 * Исход записи пакета (AWP-20, AWP-21): успех с id записанного или
 * типизированный отказ до первой вставки. Сбой после первой вставки —
 * исключение, транзакция откатывается.
 */
sealed interface AddWordsBatchOutcome {

    /**
     * [wordIds] — по слову пакета (новое или существующее),
     * [lexemeIds] — по лексемам в порядке пакета.
     */
    data class Added(
        val wordIds: List<Long>,
        val lexemeIds: List<Long>,
    ) : AddWordsBatchOutcome

    sealed interface Rejected : AddWordsBatchOutcome {

        /** Группа слова не живая или не из словаря пакета (AWP-21.3, 21.5). */
        data class GroupMissing(val groupId: Long) : Rejected

        /** Существующее слово удалено или не из словаря пакета (AWP-21.4). */
        data class WordMissing(val wordId: Long) : Rejected

        /**
         * Тип не из словаря, удалён, выключен, не принимает значение этого
         * шаблона, его опция мертва или чужая, нарушена кардинальность
         * (AWP-21.6).
         */
        data class TypeUnavailable(val typeId: Long) : Rejected

        /** У лексемы нет значения ни в одном включённом ядре (AWP-11). */
        data class NoCoreValue(val wordIndex: Int, val lexemeIndex: Int) : Rejected

        data object DictionaryMissing : Rejected
    }
}
