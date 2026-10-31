package me.apomazkin.candidates

import androidx.compose.runtime.Immutable
import me.apomazkin.lexeme.ComponentTemplate

/*
 * IS532/IS536: состояние окна лексем-кандидатов — черновик (AWP-03). Всё,
 * что показано, лежит здесь явными полями: карточки с лексемами по
 * компонентам, пометки, выборы, листы, вопросы, сводка кнопки, разделы
 * «Замечания» и «Сопоставление». Флаги не вычисляются в UI — их ставят
 * атомы редьюсеров. Имена — по вещи, без приставки модуля.
 */

/**
 * Карточка леммы (AWP-04): [lemma] — текст леммы, в окне снимка — текст
 * слова списка; [sourceWords] — слова ответа, из которых она пришла,
 * [lexemes] — лексемы-кандидаты (все, что отобрал сервер; удалённые
 * исчезают), [existingCount] — «есть в словаре: N», [formNote] — пометка
 * формы, если слово — форма этой леммы (WCL-04). «Куда» и своя группа — у
 * каждой лексемы-кандидата (AWP-18, AWP-08).
 */
@Immutable
data class LemmaCardUiItem(
    val lemma: String,
    val sourceWords: List<String>,
    val lexemes: List<CardLexemeUiItem>,
    val existingCount: Int,
    val formNote: String? = null,
)

/**
 * Лексема-кандидат в окне: [values] — значения по компонентам словаря в
 * порядке компонентов (AWP-29), [isEnglishFallback] — пометка «англ.» по
 * замечанию сервера (AWP-34), [exists] — «уже в словаре» (AWP-50) или
 * «есть» (WCL-06), [lacksCore] — «не попадёт в ядро» (AWP-11),
 * [existsInLemma] — «есть в «correr»» у раздела леммы-формы (WCL-15.4),
 * [isInContext] — употреблена в тексте снимка, пометка «в тексте» (AWP-43).
 * Плашка нейронки окна снимка: [target] — «Куда», по умолчанию «новое
 * слово» (AWP-18.3), [ownGroup] — своя группа при «новое слово» (AWP-08),
 * [isAdding] — идёт запись этой плашки (AWP-47.4), [addFailure] — запись
 * не удалась, причина под плашкой (AWP-21); диалог карточки слова эти
 * поля не читает.
 */
@Immutable
data class CardLexemeUiItem(
    val key: String,
    val source: String,
    val values: List<CandidateValueUiItem>,
    val isEnglishFallback: Boolean,
    val exists: Boolean,
    val lacksCore: Boolean,
    val existsInLemma: Boolean = false,
    val isInContext: Boolean = false,
    val target: CardTargetUi = CardTargetUi.NewWord,
    val ownGroup: CardOwnGroupUi = CardOwnGroupUi.AsCommon,
    val isAdding: Boolean = false,
    val addFailure: AddFailureUi? = null,
) {
    /** Хотя бы один показанный текст правлен руками: пометка «изменено» (AWP-16.1). */
    val isEdited: Boolean
        get() = values.any { value ->
            value.isActive && value is CandidateValueUiItem.Text && value.texts.any { it.isEdited }
        }

    /** Кнопка записи плашки активна: без «уже в словаре» и с ядром (AWP-47.3, AWP-50.12). */
    val canWrite: Boolean
        get() = !exists && !lacksCore
}

/**
 * Значение одного компонента у лексемы-кандидата. [isActive] —
 * зависимость компонента выполнена: от лексемы — всегда, от компонента —
 * у того есть значение, от опции — она выбрана (AWP-17.6); неактивное не
 * показывается и не пишется.
 */
sealed interface CandidateValueUiItem {
    val componentId: Long
    val isActive: Boolean

    /**
     * Текстовый компонент: один текст, у «несколько» — список; пустым текст
     * ядра бывает только после стирания руками (AWP-11.5); [field] — поле
     * ответа, откуда взяты тексты.
     */
    @Immutable
    data class Text(
        override val componentId: Long,
        val texts: List<EditableTextUiItem>,
        val field: String?,
        override val isActive: Boolean = true,
    ) : CandidateValueUiItem

    /** «Выбор»: опции компонента, у «несколько» — все подходящие (Д32); пусто — «без значения». */
    @Immutable
    data class Choice(
        override val componentId: Long,
        val options: List<Long>,
        override val isActive: Boolean = true,
    ) : CandidateValueUiItem

    /** У значения есть что писать: непустой текст или опция. */
    val hasContent: Boolean
        get() = when (this) {
            is Text -> texts.any { it.text.isNotBlank() }
            is Choice -> options.isNotEmpty()
        }
}

/** Правимый текст: [text] — что ляжет в словарь, [original] — текст ответа; «изменено» = text != original. */
@Immutable
data class EditableTextUiItem(
    val text: String,
    val original: String,
) {
    val isEdited: Boolean
        get() = text != original

    companion object {
        fun of(source: String): EditableTextUiItem = EditableTextUiItem(text = source, original = source)
    }
}

/** Адрес значения: лемма карточки, ключ лексемы, компонент, номер в списке текстов или опций. */
@Immutable
data class ValueRefUi(
    val lemma: String,
    val key: String,
    val componentId: Long,
    val index: Int,
)

/** Адрес компонента у лексемы-кандидата. */
@Immutable
data class ComponentRefUi(
    val lemma: String,
    val key: String,
    val componentId: Long,
)

/** «Куда» лексемы-кандидата (AWP-18): слово словаря с текстом слова или «новое слово». */
sealed interface CardTargetUi {
    data class Existing(
        val wordId: Long,
    ) : CardTargetUi

    data object NewWord : CardTargetUi
}

/** Своя группа лексемы-кандидата при «новое слово» (AWP-08): как у всех, «Все» (без группы) или группа. */
sealed interface CardOwnGroupUi {
    data object AsCommon : CardOwnGroupUi

    data object All : CardOwnGroupUi

    data class Group(
        val groupId: Long,
    ) : CardOwnGroupUi
}

/**
 * Строка леммы словаря в листе «куда класть» (AWP-18): значения ядра, число
 * лексем, группы (пусто — «Все»), день добавления в миллисекундах. Лист
 * только рисует.
 */
@Immutable
data class TargetRowUi(
    val wordId: Long,
    val coreValues: List<String>,
    val lexemeCount: Int,
    val groupNames: List<String>,
    val addedAt: Long,
)

/** Открытый лист выбора; один за раз (смена темы пересоздаёт Activity — лист в стейте). */
sealed interface CandidatesSheetUi {
    data object CommonGroup : CandidatesSheetUi

    /** Своя группа плашки по лемме карточки и ключу лексемы-кандидата (AWP-08). */
    data class CardGroup(
        val lemma: String,
        val key: String,
    ) : CandidatesSheetUi

    /** «Куда» плашки по лемме карточки и ключу лексемы-кандидата (AWP-18). */
    data class CardTarget(
        val lemma: String,
        val key: String,
    ) : CandidatesSheetUi

    /** Опция любого «Выбора» у лексемы-кандидата (AWP-17). */
    data class Option(
        val ref: ComponentRefUi,
    ) : CandidatesSheetUi

    /**
     * Правка текста по адресу значения (AWP-16): [draft] — текущий ввод,
     * [canReset] — ввод отличается от текста ответа; ставят атомы, UI сам
     * не сравнивает (AWP-16.6).
     */
    data class TextEdit(
        val ref: ValueRefUi,
        val draft: String,
        val canReset: Boolean,
    ) : CandidatesSheetUi
}

/**
 * Сводка кнопки «добавить» (AWP-13): [lexemeCount] и [wordCount] — что
 * ляжет, [existingCount] — «есть» (не пишутся), [lacksCoreCount] — без
 * ядра, [isEnabled] — кнопка активна.
 */
@Immutable
data class AddSummaryUi(
    val lexemeCount: Int = 0,
    val wordCount: Int = 0,
    val existingCount: Int = 0,
    val lacksCoreCount: Int = 0,
    val isEnabled: Boolean = false,
)

/**
 * Запись не удалась (В3, AWP-21): отказ проверки с причиной — id того, что
 * стало недействительным, — или сбой транзакции.
 */
sealed interface AddFailureUi {
    data class GroupMissing(
        val groupId: Long,
    ) : AddFailureUi

    data class WordMissing(
        val wordId: Long,
    ) : AddFailureUi

    data class TypeUnavailable(
        val typeId: Long,
    ) : AddFailureUi

    data object NoCoreValue : AddFailureUi

    data object DictionaryMissing : AddFailureUi

    data object Failed : AddFailureUi
}

/** Строка раздела «Замечания» (AWP-30); текст собирает UI из ресурсов по виду строки. */
sealed interface RemarkUiItem {
    /** «Назначение компонента «<имя>» непонятно, он оставлен пустым»; имя — по компоненту формы. */
    data class ComponentUnclear(
        val componentId: Long,
    ) : RemarkUiItem

    /** «У N значений нет перевода на <язык>, показано английское толкование»; N — разные лексемы с замечанием. */
    data class NoTranslation(
        val count: Int,
        val translationLanguage: String,
    ) : RemarkUiItem

    /** «У лексем-кандидатов есть «<имя>». Заведите в конструкторе компонент «<имя>» (<шаблон>, опции: …)…». */
    data class ComponentRecommended(
        val name: String,
        val template: ComponentTemplate,
        val options: List<String>,
    ) : RemarkUiItem
}

/** Строка раздела «Сопоставление» (AWP-31, Д41); имена компонента и опции — по форме. */
sealed interface ComponentMatchLineUiItem {
    /** «<поле ответа> → <компонент>». */
    data class FieldToComponent(
        val field: String,
        val componentId: Long,
    ) : ComponentMatchLineUiItem

    /** «<компонент>: <подпись опции>». */
    data class ComponentOption(
        val componentId: Long,
        val optionId: Long,
    ) : ComponentMatchLineUiItem
}
