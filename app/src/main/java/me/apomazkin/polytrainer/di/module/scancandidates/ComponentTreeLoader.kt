package me.apomazkin.polytrainer.di.module.scancandidates

import androidx.annotation.StringRes
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentOptionApiEntity
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentOptionNodeApiEntity
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.lexeme.PartOfSpeechOption
import me.apomazkin.ui.resource.ResourceManager
import javax.inject.Inject

/**
 * Включённый живой компонент словаря и всё, что от него зависит: [options] —
 * живые опции «Выбора» вместе с зависимыми от каждой, [children] — зависимые от
 * самого компонента. Пока у компонента-родителя нет значения или выбрана не та
 * опция, зависимый не доступен (IS486), поэтому ветви сидят под тем, от чего
 * зависят.
 */
data class ComponentBranch(
    val type: ComponentTypeApiEntity,
    val options: List<OptionBranch>,
    val children: List<ComponentBranch>,
)

/** Живая опция «Выбора» и компоненты, зависящие от её выбора. */
data class OptionBranch(
    val option: ComponentOptionApiEntity,
    val children: List<ComponentBranch>,
)

/**
 * Компоненты словаря деревом зависимостей (WFT-78). Корень — включённые
 * компоненты, зависящие от самой лексемы; потомки — по тому, от чего зависят.
 * Выключенный компонент и всё под ним в дерево не входит: пока родителя нет, от
 * него ничего не зависит (WFT-78.6). Порядок на каждом уровне: встроенные
 * раньше своих, затем `position`, затем id — как порядок значений в карточке
 * слова; опции — по `position`, затем id.
 *
 * Чистая функция: данные уже прочитаны, имён и ресурсов в ней нет.
 */
fun forestOf(
    types: List<ComponentTypeApiEntity>,
    optionsByType: Map<Long, List<ComponentOptionApiEntity>>,
): List<ComponentBranch> {
    val live = types
        .filter { it.enabled && it.removedAt == null }
        .sortedWith(compareBy({ it.systemKey == null }, { it.position }, { it.id }))

    fun dependingOn(target: DependencyTarget): List<ComponentTypeApiEntity> =
        live.filter { it.dependsOn == target }

    fun branchOf(type: ComponentTypeApiEntity): ComponentBranch = ComponentBranch(
        type = type,
        options = optionsByType[type.id]
            .orEmpty()
            .filter { it.removedAt == null }
            .sortedWith(compareBy({ it.position }, { it.id }))
            .map { option ->
                OptionBranch(
                    option = option,
                    children = dependingOn(DependencyTarget.Option(option.id)).map(::branchOf),
                )
            },
        children = dependingOn(DependencyTarget.Component(ComponentTypeId(type.id)))
            .map(::branchOf),
    )

    return dependingOn(DependencyTarget.Lexeme).map(::branchOf)
}

/**
 * Дерево плоским списком в порядке обхода: компонент, затем зависимые от его
 * опций (по порядку опций), затем зависимые от него самого. Так порядок
 * компонентов в форме окна совпадает с порядком в запросе.
 */
fun List<ComponentBranch>.flattened(): List<ComponentBranch> = flatMap { branch ->
    listOf(branch) +
        branch.options.flatMap { it.children.flattened() } +
        branch.children.flattened()
}

/**
 * Читает компоненты словаря и собирает из них дерево для запроса к серверу слов
 * и порядок компонентов для формы окна (WFT-78). Имена встроенных компонентов и
 * опций — на языке пользователя, из ресурсов; у своих — как назвал пользователь.
 */
class ComponentTreeLoader @Inject constructor(
    private val lexemeApi: CoreDbApi.LexemeApi,
    private val resources: ResourceManager,
) {
    /**
     * Дерево компонентов словаря для запроса: узлы как `Component` в контракте.
     * Пустое — в словаре нет включённых компонентов (WFT-78.7).
     */
    suspend fun treeOf(dictionaryId: Long): List<ComponentNodeApiEntity> =
        componentsOf(dictionaryId).map { it.toNode() }

    /** Корни дерева: включённые живые компоненты, зависящие от лексемы. */
    suspend fun componentsOf(dictionaryId: Long): List<ComponentBranch> {
        val types = lexemeApi.getComponentTypes(dictionaryId)
        val options = types
            .filter { it.enabled && it.template == ComponentTemplate.CHOICE }
            .associate { it.id to lexemeApi.getComponentOptions(it.id) }
        return forestOf(types, options)
    }

    private fun ComponentBranch.toNode(): ComponentNodeApiEntity = ComponentNodeApiEntity(
        id = type.id,
        key = type.systemKey?.key,
        name = type.displayName(),
        template = type.template.key,
        core = type.core,
        multiple = type.isMultiple,
        options = options.map { it.toNode() },
        children = children.map { it.toNode() },
    )

    private fun OptionBranch.toNode(): ComponentOptionNodeApiEntity = ComponentOptionNodeApiEntity(
        id = option.id,
        key = option.systemKey,
        label = option.displayLabel(),
        children = children.map { it.toNode() },
    )

    /** Встроенный компонент — ресурсом, свой — как назвал пользователь. */
    private fun ComponentTypeApiEntity.displayName(): String =
        systemKey?.let { resources.stringByResId(it.nameRes()) } ?: name.orEmpty()

    /** Опция: своя или переименованная — её текст, встроенная — ресурс по ключу. */
    private fun ComponentOptionApiEntity.displayLabel(): String {
        label?.let { return it }
        val key = systemKey ?: return ""
        val option = PartOfSpeechOption.entries.firstOrNull { it.key == key }
        return option?.let { resources.stringByResId(it.labelRes()) } ?: key
    }
}

@StringRes
private fun BuiltInComponent.nameRes(): Int = when (this) {
    BuiltInComponent.TRANSLATION -> R.string.word_card_bottom_translation
    BuiltInComponent.PART_OF_SPEECH -> R.string.builtin_component_part_of_speech
    BuiltInComponent.EXAMPLE -> R.string.builtin_component_example
}

@StringRes
private fun PartOfSpeechOption.labelRes(): Int = when (this) {
    PartOfSpeechOption.NOUN -> R.string.part_of_speech_option_noun
    PartOfSpeechOption.VERB -> R.string.part_of_speech_option_verb
    PartOfSpeechOption.ADJECTIVE -> R.string.part_of_speech_option_adjective
    PartOfSpeechOption.ADVERB -> R.string.part_of_speech_option_adverb
    PartOfSpeechOption.PRONOUN -> R.string.part_of_speech_option_pronoun
    PartOfSpeechOption.NUMERAL -> R.string.part_of_speech_option_numeral
    PartOfSpeechOption.PREPOSITION -> R.string.part_of_speech_option_preposition
    PartOfSpeechOption.INTERJECTION -> R.string.part_of_speech_option_interjection
    PartOfSpeechOption.PHRASAL_VERB -> R.string.part_of_speech_option_phrasal_verb
    PartOfSpeechOption.COLLOCATION -> R.string.part_of_speech_option_collocation
    PartOfSpeechOption.IDIOM -> R.string.part_of_speech_option_idiom
    PartOfSpeechOption.PHRASE -> R.string.part_of_speech_option_phrase
}
