package me.apomazkin.lexeme

/*
 * IS536: форма словаря для окна лексем-кандидатов (AWP Ш1) и диалога
 * карточки слова (WCL Ш1) — домен, которым говорят оба порта.
 */

/** Исход чтения формы: форма, словарь удалён (AWP-24.4), чтение упало (AWP-24.1). */
sealed interface FormOutcome {
    data class Loaded(
        val form: DictionaryForm,
    ) : FormOutcome

    data object DictionaryMissing : FormOutcome

    /** [cause] — для лога, не для пользователя. */
    data class Failed(
        val cause: String,
    ) : FormOutcome
}

/**
 * Форма словаря: [components] — включённые живые компоненты в порядке
 * дерева (корни по позиции, зависимые сразу за тем, от чего зависят),
 * [options] — живые опции каждого «Выбора» по id компонента (AWP-17),
 * [groups] — живые группы, [existingLemmas] — леммы словаря с текстами
 * лемм ответа (AWP-07, AWP-18).
 */
data class DictionaryForm(
    val dictionaryId: Long,
    val translationLanguage: String,
    val components: List<FormComponent>,
    val options: Map<Long, List<FormOption>>,
    val groups: List<FormGroup>,
    val existingLemmas: List<ExistingLemma>,
)

/** Компонент словаря: [systemKey] у встроенного (имя даёт UI), [name] у своего, [dependsOn] — от чего зависит. */
data class FormComponent(
    val id: Long,
    val systemKey: BuiltInComponent?,
    val name: String?,
    val template: ComponentTemplate,
    val isCore: Boolean,
    val isMultiple: Boolean,
    val position: Int,
    val dependsOn: DependencyTarget,
)

/** Живая опция «Выбора»: [key] встроенной опции (`verb`), [label] своей. */
data class FormOption(
    val id: Long,
    val key: String?,
    val label: String?,
    val position: Int,
)

data class FormGroup(
    val id: Long,
    val name: String,
)

/**
 * Лемма словаря с текстом леммы ответа (AWP-18): [coreValues] — первые
 * значения лексем в первом включённом ядре шаблона «текст»;
 * [lexemeTextKeys] — ключи значений ядер шаблона «текст» всех её лексем
 * (`lexemeTextKey`) для пометки «есть» (AWP-07); [groupNames] — её группы.
 */
data class ExistingLemma(
    val wordId: Long,
    val text: String,
    val lexemeCount: Int,
    val coreValues: List<String>,
    val lexemeTextKeys: Set<String>,
    val groupNames: List<String>,
)
