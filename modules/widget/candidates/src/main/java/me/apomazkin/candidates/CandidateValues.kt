package me.apomazkin.candidates

import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget

/*
 * IS536: значения лексемы-кандидата по сопоставлению (AWP-29) и их
 * активность по зависимостям компонентов (AWP-17.6) — чистые функции над
 * UI-типами.
 */

/** Поля ответа, которые нейронка кладёт в текстовые компоненты (LFL-87, «Связи»). */
private const val FIELD_TRANSLATIONS = "translations"
private const val FIELD_GLOSS = "gloss"
private const val FIELD_EXAMPLES = "examples"
private const val FIELD_NOTES = "notes"
private const val FIELD_CAPTION = "caption"
private const val FIELD_SYNONYMS = "synonyms"
private const val FIELD_ANTONYMS = "antonyms"
private const val FIELD_LEVEL = "level"
private const val FIELD_PRONUNCIATIONS = "pronunciations"
private const val GRAMMAR_PREFIX = "grammar."

/** Список в компоненте с одним значением — элементы через «, » (AWP-29.9). */
private const val LIST_SEPARATOR = ", "

/**
 * Значения лексемы по сопоставлению в порядке компонентов формы (AWP-29):
 * строка с полем → текст из поля ответа, список без «несколько» — одной
 * строкой через «, »; строка с опцией → опция «Выбора», мёртвая опция
 * отбрасывается (AWP-29.13); строки на компоненты не из формы
 * отбрасываются (AWP-29.12); компонент без строк значения не получает.
 */
fun valuesOf(
    lexeme: LookupLexemeUi,
    lemma: LookupLemmaUi,
    form: DictionaryFormUi,
): List<CandidateValueUiItem> = form.components.mapNotNull { component -> valueOf(component, lexeme, lemma, form) }

private fun valueOf(
    component: FormComponentUi,
    lexeme: LookupLexemeUi,
    lemma: LookupLemmaUi,
    form: DictionaryFormUi,
): CandidateValueUiItem? {
    val matches = lexeme.componentMatches.filter { it.componentId == component.id }
    if (matches.isEmpty()) return null
    return when (component.template) {
        ComponentTemplate.CHOICE -> {
            val live = form.optionsOf(component.id).map { it.id }.toSet()
            val options = matches.mapNotNull { it.optionId }.filter { it in live }.distinct()
            CandidateValueUiItem.Choice(componentId = component.id, options = options)
        }
        ComponentTemplate.TEXT, ComponentTemplate.CAPTIONED_TEXT -> {
            val fields = matches.mapNotNull { it.field }
            val texts = fields.flatMap { field -> textsOf(field, lexeme, lemma, component.isMultiple) }
            if (texts.isEmpty()) return null
            CandidateValueUiItem.Text(
                componentId = component.id,
                texts = texts.map { EditableTextUiItem.of(it) },
                field = fields.firstOrNull(),
            )
        }
        ComponentTemplate.IMAGE -> null
    }
}

/** Тексты поля ответа: у «несколько» — элементами, иначе одной строкой через «, »; пустых нет. */
private fun textsOf(
    field: String,
    lexeme: LookupLexemeUi,
    lemma: LookupLemmaUi,
    isMultiple: Boolean,
): List<String> {
    val items = fieldItemsOf(field, lexeme, lemma).filter { it.isNotBlank() }
    if (items.isEmpty()) return emptyList()
    return if (isMultiple) items else listOf(items.joinToString(LIST_SEPARATOR))
}

private fun fieldItemsOf(
    field: String,
    lexeme: LookupLexemeUi,
    lemma: LookupLemmaUi,
): List<String> = when (field) {
    FIELD_TRANSLATIONS -> lexeme.translations
    FIELD_GLOSS -> listOfNotNull(lexeme.gloss)
    FIELD_EXAMPLES -> lexeme.examples
    FIELD_NOTES -> lexeme.notes
    FIELD_CAPTION -> listOfNotNull(lexeme.caption)
    FIELD_SYNONYMS -> lexeme.synonyms
    FIELD_ANTONYMS -> lexeme.antonyms
    FIELD_LEVEL -> listOfNotNull(lexeme.level)
    FIELD_PRONUNCIATIONS -> lemma.pronunciations
    else -> listOfNotNull(lexeme.grammar[field.removePrefix(GRAMMAR_PREFIX)].takeIf { field.startsWith(GRAMMAR_PREFIX) })
}

/**
 * Активность значений по зависимостям компонентов формы (AWP-17.6, 17.7):
 * от лексемы — всегда, от компонента — у того есть активное значение с
 * содержимым, от опции — она выбрана в активном «Выборе».
 */
fun List<LemmaCardUiItem>.withActiveValues(form: DictionaryFormUi): List<LemmaCardUiItem> = map { card ->
    card.copy(lexemes = card.lexemes.map { it.withActiveValues(form) })
}

private fun CardLexemeUiItem.withActiveValues(form: DictionaryFormUi): CardLexemeUiItem {
    val byComponent = values.associateBy { it.componentId }
    val decided = mutableMapOf<Long, Boolean>()
    val visiting = mutableSetOf<Long>()

    fun isActive(componentId: Long): Boolean {
        decided[componentId]?.let { return it }
        if (!visiting.add(componentId)) return false
        val component = form.componentOf(componentId)
        val value = byComponent[componentId]
        val active = when {
            component == null || value == null -> false
            else -> when (val target = component.dependsOn) {
                DependencyTarget.Lexeme -> true
                is DependencyTarget.Component -> {
                    val parent = byComponent[target.typeId.id]
                    parent != null && parent.hasContent && isActive(target.typeId.id)
                }
                is DependencyTarget.Option -> {
                    val owner = form.ownerOfOption(target.optionId)
                    val chosen = owner?.let { byComponent[it.id] as? CandidateValueUiItem.Choice }
                    owner != null && chosen != null && target.optionId in chosen.options && isActive(owner.id)
                }
            }
        }
        visiting.remove(componentId)
        decided[componentId] = active
        return active
    }

    return copy(values = values.map { it.withActive(isActive(it.componentId)) })
}

private fun CandidateValueUiItem.withActive(isActive: Boolean): CandidateValueUiItem = when (this) {
    is CandidateValueUiItem.Text -> copy(isActive = isActive)
    is CandidateValueUiItem.Choice -> copy(isActive = isActive)
}
