package me.apomazkin.wordstab.logic.photo

import androidx.compose.runtime.Immutable
import me.apomazkin.wordstab.deps.PhotoDataKind

/*
 * IS532: состояние окна добавления — черновик (AWP-03). Всё, что показано,
 * лежит здесь явными полями: карточки с лексемами-кандидатами, пометки,
 * выборы, листы, вопросы, сводка кнопки. Флаги не вычисляются в UI —
 * их ставят атомы reducer'а.
 */

/**
 * Карточка леммы (AWP-04): [lemma] — текст леммы, [sourceWords] — слова
 * ответа, из которых она пришла, [lexemes] — показанные лексемы-кандидаты
 * (до пяти, удалённые исчезают), [hiddenCount] — «ещё N» (не меняется
 * при удалениях, AWP-05.4), [existingCount] — «есть в словаре: N»,
 * [target] — куда класть (AWP-18), [ownGroup] — своя группа (AWP-08).
 */
@Immutable
data class PhotoCard(
        val lemma: String,
        val sourceWords: List<String>,
        val lexemes: List<PhotoCardLexeme>,
        val hiddenCount: Int,
        val existingCount: Int,
        val target: PhotoCardTarget,
        val ownGroup: PhotoOwnGroup,
        val isExpanded: Boolean,
)

/**
 * Лексема-кандидат в окне: [translation] — правимый текст перевода
 * (AWP-10, AWP-16), [isEnglishFallback] — переводов нет, текст — толкование
 * с пометкой «англ.» (AWP-10.3), [gloss] — правимое толкование под текстом
 * или null, [examples] — оставшиеся примеры, [partOfSpeechOptionId] —
 * опция «Части речи» или null («без части речи», AWP-17), [exists] —
 * «есть» (AWP-07), [lacksCore] — «не попадёт в ядро» (AWP-11).
 */
@Immutable
data class PhotoCardLexeme(
        val key: String,
        val source: String,
        val translation: PhotoEditableText,
        /** У сервера были переводы; иначе текст — толкование (AWP-22.3…22.5). */
        val hasOwnTranslations: Boolean,
        val isEnglishFallback: Boolean,
        val gloss: PhotoEditableText?,
        val caption: String?,
        val notes: List<String>,
        val examples: List<PhotoEditableText>,
        val partOfSpeechOptionId: Long?,
        val parent: Boolean,
        val parentKey: String?,
        val exists: Boolean,
        val lacksCore: Boolean,
)

/** Правимый текст: [text] — что ляжет в словарь, [original] — текст источника; «изменено» = text != original. */
@Immutable
data class PhotoEditableText(
        val text: String,
        val original: String,
) {
    val isEdited: Boolean
        get() = text != original

    companion object {
        fun of(source: String): PhotoEditableText = PhotoEditableText(text = source, original = source)
    }
}

/** Куда класть лексемы карточки (AWP-18). */
sealed interface PhotoCardTarget {
    data class Existing(val wordId: Long) : PhotoCardTarget

    data object NewWord : PhotoCardTarget
}

/** Своя группа карточки (AWP-08): как у всех, «Все» (без группы) или группа. */
sealed interface PhotoOwnGroup {
    data object AsCommon : PhotoOwnGroup

    data object All : PhotoOwnGroup

    data class Group(val groupId: Long) : PhotoOwnGroup
}

/** Слово без добавления (AWP-12): не найдено, не загрузилось, без значений. */
@Immutable
data class PhotoNotAdded(
        val text: String,
        val reason: PhotoNotAddedReason,
)

enum class PhotoNotAddedReason {
    NOT_FOUND,
    ERROR,
    NO_LEXEMES,
}

/** Открытый лист выбора; один за раз (смена темы пересоздаёт Activity — лист в State). */
sealed interface PhotoSheet {
    data object CommonGroup : PhotoSheet

    data class CardGroup(val lemma: String) : PhotoSheet

    data class CardTarget(val lemma: String) : PhotoSheet

    data class PartOfSpeech(val lemma: String, val key: String) : PhotoSheet

    data class MappingTarget(val kind: PhotoDataKind) : PhotoSheet

    /** Правка текста: [field] — какой текст, [draft] — текущий ввод. */
    data class TextEdit(
            val lemma: String,
            val key: String,
            val field: PhotoTextField,
            val draft: String,
    ) : PhotoSheet
}

/** Какой текст лексемы-кандидата правится (AWP-16). */
sealed interface PhotoTextField {
    data object Translation : PhotoTextField

    data object Gloss : PhotoTextField

    data class Example(val index: Int) : PhotoTextField
}

/**
 * Сводка кнопки «добавить» (AWP-13), пересчитывается атомом после каждого
 * действия: [lexemeCount] и [wordCount] — что ляжет, [existingCount] —
 * «есть» (не пишутся), [lacksCoreCount] — без ядра, [noCoreMapping] — ни
 * один вид данных не идёт в ядро (AWP-11.7), [isEnabled] — кнопка активна.
 */
@Immutable
data class PhotoAddSummary(
        val lexemeCount: Int = 0,
        val wordCount: Int = 0,
        val existingCount: Int = 0,
        val lacksCoreCount: Int = 0,
        val noCoreMapping: Boolean = false,
        val isEnabled: Boolean = false,
)

/** Причина, по которой окно не собралось (В1, AWP-24). */
enum class PhotoFormError {
    DICTIONARY_MISSING,
    LOAD_FAILED,
}

/**
 * Запись не удалась (В3, AWP-21): отказ проверки с причиной — id того, что
 * стало недействительным, — или сбой транзакции.
 */
sealed interface PhotoAddFailure {
    data class GroupMissing(val groupId: Long) : PhotoAddFailure

    data class WordMissing(val wordId: Long) : PhotoAddFailure

    data class TypeUnavailable(val typeId: Long) : PhotoAddFailure

    data object NoCoreValue : PhotoAddFailure

    data object DictionaryMissing : PhotoAddFailure

    data object Failed : PhotoAddFailure
}
