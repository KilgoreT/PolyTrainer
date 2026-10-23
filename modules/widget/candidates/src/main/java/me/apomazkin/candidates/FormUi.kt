package me.apomazkin.candidates

import androidx.compose.runtime.Immutable
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget

/*
 * IS536: форма словаря глазами экранов — UI-двойник домена
 * `DictionaryForm`, поле в поле (`mapper/FormMappers.kt`).
 */

/**
 * Форма словаря: [components] — включённые живые компоненты в порядке
 * дерева, [options] — живые опции каждого «Выбора» по id компонента
 * (AWP-17), [groups] — живые группы, [existingLemmas] — леммы словаря с
 * текстами лемм ответа (AWP-07, AWP-18).
 */
@Immutable
data class DictionaryFormUi(
    val dictionaryId: Long,
    val translationLanguage: String,
    val components: List<FormComponentUi>,
    val options: Map<Long, List<FormOptionUi>>,
    val groups: List<FormGroupUi>,
    val existingLemmas: List<ExistingLemmaUi>,
)

/** Компонент словаря: [systemKey] у встроенного (имя даёт UI), [name] у своего, [dependsOn] — от чего зависит. */
@Immutable
data class FormComponentUi(
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
@Immutable
data class FormOptionUi(
    val id: Long,
    val key: String?,
    val label: String?,
    val position: Int,
)

@Immutable
data class FormGroupUi(
    val id: Long,
    val name: String,
)

/**
 * Лемма словаря с текстом леммы ответа (AWP-18): [coreValues] — первые
 * значения лексем в первом включённом ядре шаблона «текст»;
 * [lexemeTextKeys] — ключи значений ядер шаблона «текст» всех её лексем
 * для пометки «есть» (AWP-07); [groupNames] — её группы.
 */
@Immutable
data class ExistingLemmaUi(
    val wordId: Long,
    val text: String,
    val lexemeCount: Int,
    val coreValues: List<String>,
    val lexemeTextKeys: Set<String>,
    val groupNames: List<String>,
)

/** Компонент формы по id; null — его в форме нет. */
fun DictionaryFormUi.componentOf(componentId: Long): FormComponentUi? = components.firstOrNull { it.id == componentId }

/** Живые опции «Выбора» по id компонента; пусто у остальных. */
fun DictionaryFormUi.optionsOf(componentId: Long): List<FormOptionUi> = options[componentId].orEmpty()

/** Компонент-владелец опции; null — опции нет ни у одного «Выбора» формы. */
fun DictionaryFormUi.ownerOfOption(optionId: Long): FormComponentUi? =
    components.firstOrNull { component -> optionsOf(component.id).any { it.id == optionId } }
