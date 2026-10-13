package me.apomazkin.wordstab.deps

import me.apomazkin.lexeme.BuiltInComponent

/*
 * IS532: форма словаря снимка, соответствия и исходы окна добавления
 * глазами экрана (Ш1, Ш4). Сущности базы сюда не доходят — маппинг в
 * `app`. Порядок вариантов sealed — по важности.
 */

/** Исход загрузки формы словаря снимка (Ш1 → Ш2 или В1). */
sealed interface PhotoFormOutcome {

    /** Форма загружена; [remembered] — соответствия прошлого снимка на этом словаре или null. */
    data class Loaded(
            val form: PhotoDictionaryForm,
            val remembered: PhotoMapping?,
    ) : PhotoFormOutcome

    /** Словарь снимка удалён, пока шёл запрос (AWP-01.2, AWP-24.4). */
    data object DictionaryMissing : PhotoFormOutcome

    /** Чтение формы или лемм бросило ошибку (AWP-24.1); [cause] — для лога. */
    data class Failed(val cause: String) : PhotoFormOutcome
}

/**
 * Форма словаря снимка: включённые живые компоненты, зависящие от лексемы;
 * живые опции «Части речи» (пусто, если она выключена); живые группы;
 * леммы словаря с текстом лемм ответа (AWP-07, AWP-18).
 */
data class PhotoDictionaryForm(
        val dictionaryId: Long,
        val translationLanguage: String,
        val components: List<PhotoComponent>,
        val partOfSpeechOptions: List<PhotoPartOfSpeechOption>,
        val groups: List<PhotoGroup>,
        val existingLemmas: List<PhotoExistingLemma>,
)

/** Компонент словаря: [systemKey] у встроенного (имя даёт UI), [name] у своего. */
data class PhotoComponent(
        val id: Long,
        val systemKey: BuiltInComponent?,
        val name: String?,
        val template: PhotoTemplate,
        val isCore: Boolean,
        val isMultiple: Boolean,
        val position: Int,
)

enum class PhotoTemplate {
    TEXT,
    CHOICE,
    CAPTIONED_TEXT,
    IMAGE,
}

/** Живая опция «Части речи»: [key] встроенной опции (`verb`), [label] своей. */
data class PhotoPartOfSpeechOption(
        val id: Long,
        val key: String?,
        val label: String?,
        val position: Int,
)

data class PhotoGroup(
        val id: Long,
        val name: String,
)

/**
 * Лемма словаря с текстом леммы ответа (AWP-18): [coreValues] — первые
 * значения лексем в первом включённом ядре шаблона «текст»;
 * [lexemeTextKeys] — ключи всех значений шаблона «текст» всех её лексем
 * (`lexemeTextKey`) для пометки «есть» (AWP-07); [groupNames] — её группы.
 */
data class PhotoExistingLemma(
        val wordId: Long,
        val text: String,
        val lexemeCount: Int,
        val coreValues: List<String>,
        val lexemeTextKeys: Set<String>,
        val groupNames: List<String>,
)

/** Вид данных лексемы-кандидата (AWP-09); порядок — порядок разбора умолчаний (AWP-28). */
enum class PhotoDataKind {
    TRANSLATION,
    GLOSS,
    EXAMPLES,
    PART_OF_SPEECH,
}

/** Цель вида данных: компонент словаря по id или «не добавлять». */
sealed interface PhotoTarget {
    data class Component(val componentId: Long) : PhotoTarget

    data object None : PhotoTarget
}

/** Соответствия окна (AWP-09): по цели на каждый вид данных. */
data class PhotoMapping(
        val translation: PhotoTarget,
        val gloss: PhotoTarget,
        val examples: PhotoTarget,
        val partOfSpeech: PhotoTarget,
) {
    fun targetOf(kind: PhotoDataKind): PhotoTarget = when (kind) {
        PhotoDataKind.TRANSLATION -> translation
        PhotoDataKind.GLOSS -> gloss
        PhotoDataKind.EXAMPLES -> examples
        PhotoDataKind.PART_OF_SPEECH -> partOfSpeech
    }

    fun with(kind: PhotoDataKind, target: PhotoTarget): PhotoMapping = when (kind) {
        PhotoDataKind.TRANSLATION -> copy(translation = target)
        PhotoDataKind.GLOSS -> copy(gloss = target)
        PhotoDataKind.EXAMPLES -> copy(examples = target)
        PhotoDataKind.PART_OF_SPEECH -> copy(partOfSpeech = target)
    }

    companion object {
        val NONE = PhotoMapping(
                translation = PhotoTarget.None,
                gloss = PhotoTarget.None,
                examples = PhotoTarget.None,
                partOfSpeech = PhotoTarget.None,
        )
    }
}
