package me.apomazkin.candidates

import me.apomazkin.lexeme.ComponentTemplate

/*
 * IS536: разделы «Замечания» (AWP-30) и «Сопоставление» (AWP-31) —
 * строки из замечаний ответа и сопоставлений всех лексем; чистые
 * функции, которые зовут атомы обоих окон.
 */

/**
 * Строки «Замечаний» (AWP-30): сначала по замечаниям слов в порядке слов —
 * `component_unclear` одной строкой на компонент формы (нет в форме —
 * строки нет), `no_translation` одной строкой на окно на месте первого
 * замечания со счётом разных лексем; затем рекомендации ответа.
 */
fun remarksOf(
    words: List<LookupWordUi>,
    recommendations: List<ComponentRecommendationUi>,
    form: DictionaryFormUi,
): List<RemarkUiItem> {
    val lines = mutableListOf<RemarkUiItem>()
    val unclear = mutableSetOf<Long>()
    val withoutTranslation = mutableSetOf<Pair<String, String>>()
    var noTranslationAt = -1
    for (word in words) {
        for (remark in word.remarks) {
            when (remark) {
                is WordRemarkUi.ComponentUnclear -> {
                    if (form.componentOf(remark.componentId) != null && unclear.add(remark.componentId)) {
                        lines += RemarkUiItem.ComponentUnclear(remark.componentId)
                    }
                }
                is WordRemarkUi.NoTranslation -> {
                    if (noTranslationAt < 0) {
                        noTranslationAt = lines.size
                        lines += RemarkUiItem.NoTranslation(count = 0, translationLanguage = form.translationLanguage)
                    }
                    withoutTranslation += remark.lemma to remark.lexemeKey
                }
            }
        }
    }
    if (noTranslationAt >= 0) {
        lines[noTranslationAt] = RemarkUiItem.NoTranslation(
            count = withoutTranslation.size,
            translationLanguage = form.translationLanguage,
        )
    }
    recommendations.forEach { recommendation ->
        lines += RemarkUiItem.ComponentRecommended(
            name = recommendation.name,
            template = recommendation.template,
            options = recommendation.options,
        )
    }
    return lines
}

/**
 * Строки «Сопоставления» (AWP-31, Д41): по сопоставлениям всех лексем
 * ответа без повторов, в порядке компонентов словаря, внутри компонента —
 * в порядке первого появления; у текстового — «поле → компонент», у
 * «Выбора» — «компонент: опция»; строки на компоненты и опции не из формы
 * отбрасываются.
 */
fun componentMatchLinesOf(
    words: List<LookupWordUi>,
    form: DictionaryFormUi,
): List<ComponentMatchLineUiItem> {
    val matches = words.flatMap { word -> word.lemmas.flatMap { lemma -> lemma.lexemes.flatMap { it.componentMatches } } }
    return form.components.flatMap { component ->
        val own = matches.filter { it.componentId == component.id }
        when (component.template) {
            ComponentTemplate.CHOICE -> {
                val live = form.optionsOf(component.id).map { it.id }.toSet()
                own
                    .mapNotNull { it.optionId }
                    .filter { it in live }
                    .distinct()
                    .map { ComponentMatchLineUiItem.ComponentOption(componentId = component.id, optionId = it) }
            }
            ComponentTemplate.TEXT, ComponentTemplate.CAPTIONED_TEXT ->
                own
                    .mapNotNull { it.field }
                    .distinct()
                    .map { ComponentMatchLineUiItem.FieldToComponent(field = it, componentId = component.id) }
            ComponentTemplate.IMAGE -> emptyList()
        }
    }
}
