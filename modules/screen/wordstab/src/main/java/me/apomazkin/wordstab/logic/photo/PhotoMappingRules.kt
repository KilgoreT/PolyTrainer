package me.apomazkin.wordstab.logic.photo

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.wordstab.deps.PhotoComponent
import me.apomazkin.wordstab.deps.PhotoDataKind
import me.apomazkin.wordstab.deps.PhotoDictionaryForm
import me.apomazkin.wordstab.deps.PhotoMapping
import me.apomazkin.wordstab.deps.PhotoTarget
import me.apomazkin.wordstab.deps.PhotoTemplate

/*
 * IS532: правила соответствий (AWP-06, AWP-09, AWP-28) — чистые функции
 * над формой словаря. Компоненты формы уже отфильтрованы use case'ом:
 * включённые, живые, зависящие от лексемы.
 */

/** Компоненты, которые может принять вид данных (AWP-09); порядок формы. */
internal fun PhotoDictionaryForm.targetsFor(kind: PhotoDataKind): List<PhotoComponent> = when (kind) {
    PhotoDataKind.TRANSLATION, PhotoDataKind.GLOSS -> components.filter { it.template == PhotoTemplate.TEXT }
    PhotoDataKind.EXAMPLES -> components.filter { it.template == PhotoTemplate.CAPTIONED_TEXT && it.isMultiple }
    PhotoDataKind.PART_OF_SPEECH -> components.filter { it.systemKey == BuiltInComponent.PART_OF_SPEECH }
}

/**
 * Умолчание вида данных (AWP-06): перевод — «Перевод», если он включён,
 * иначе первое ядро шаблона «текст» (встроенные раньше своих, затем
 * position, затем id); толкование — не добавлять; часть речи — «Часть
 * речи»; примеры — «Пример». Нет компонента — не добавлять.
 */
internal fun PhotoDictionaryForm.defaultTarget(kind: PhotoDataKind): PhotoTarget = when (kind) {
    PhotoDataKind.TRANSLATION -> builtIn(BuiltInComponent.TRANSLATION) ?: firstTextCore()
    PhotoDataKind.GLOSS -> PhotoTarget.None
    PhotoDataKind.EXAMPLES -> builtIn(BuiltInComponent.EXAMPLE) ?: PhotoTarget.None
    PhotoDataKind.PART_OF_SPEECH -> builtIn(BuiltInComponent.PART_OF_SPEECH) ?: PhotoTarget.None
}

private fun PhotoDictionaryForm.builtIn(key: BuiltInComponent): PhotoTarget? =
    components.firstOrNull { it.systemKey == key }?.let { PhotoTarget.Component(it.id) }

private fun PhotoDictionaryForm.firstTextCore(): PhotoTarget = components
    .filter { it.template == PhotoTemplate.TEXT && it.isCore }
    .sortedWith(compareBy({ it.systemKey == null }, { it.position }, { it.id }))
    .firstOrNull()
    ?.let { PhotoTarget.Component(it.id) }
    ?: PhotoTarget.None

/**
 * Соответствия окна из запомненных (AWP-06): запомненный компонент
 * действителен, если он среди целей вида; запомненное «не добавлять»
 * остаётся (AWP-06.7); иначе — умолчание вида. Затем один компонент —
 * один вид (AWP-28): запомненный выбор сильнее умолчания, между
 * умолчаниями виды разбираются в порядке [PhotoDataKind].
 */
internal fun PhotoDictionaryForm.resolveMapping(remembered: PhotoMapping?): PhotoMapping {
    val chosen = PhotoDataKind.entries.associateWith { kind -> choiceOf(kind, remembered?.targetOf(kind)) }
    val taken = mutableSetOf<Long>()
    var mapping = PhotoMapping.NONE
    for (kind in PhotoDataKind.entries.filter { chosen.getValue(it).isRemembered }) {
        mapping = mapping.with(kind, claim(chosen.getValue(kind).target, taken))
    }
    for (kind in PhotoDataKind.entries.filterNot { chosen.getValue(it).isRemembered }) {
        mapping = mapping.with(kind, claim(chosen.getValue(kind).target, taken))
    }
    return mapping
}

private class Choice(val target: PhotoTarget, val isRemembered: Boolean)

private fun PhotoDictionaryForm.choiceOf(kind: PhotoDataKind, remembered: PhotoTarget?): Choice = when (remembered) {
    null -> Choice(defaultTarget(kind), isRemembered = false)
    PhotoTarget.None -> Choice(PhotoTarget.None, isRemembered = true)
    is PhotoTarget.Component -> if (targetsFor(kind).any { it.id == remembered.componentId }) {
        Choice(remembered, isRemembered = true)
    } else {
        Choice(defaultTarget(kind), isRemembered = false)
    }
}

private fun claim(target: PhotoTarget, taken: MutableSet<Long>): PhotoTarget {
    if (target !is PhotoTarget.Component) return target
    if (!taken.add(target.componentId)) return PhotoTarget.None
    return target
}

/**
 * Выбор пользователя (AWP-28.1): компонент, занятый другим видом,
 * переводит тот вид в «не добавлять».
 */
internal fun PhotoMapping.choose(kind: PhotoDataKind, target: PhotoTarget): PhotoMapping {
    var mapping = with(kind, target)
    if (target is PhotoTarget.Component) {
        for (other in PhotoDataKind.entries.filter { it != kind }) {
            if (mapping.targetOf(other) == target) mapping = mapping.with(other, PhotoTarget.None)
        }
    }
    return mapping
}

/** Ни один вид данных не идёт в ядро — сводка красная (AWP-11.7, 11.8, AWP-06.4). */
internal fun PhotoMapping.hasNoCore(form: PhotoDictionaryForm): Boolean =
    listOf(translation, gloss, examples).none { it.isCore(form) }

private fun PhotoTarget.isCore(form: PhotoDictionaryForm): Boolean =
    this is PhotoTarget.Component && form.components.any { it.id == componentId && it.isCore }

/** Компонент формы по цели; «не добавлять» или чужой id — null. */
internal fun PhotoDictionaryForm.componentOf(target: PhotoTarget): PhotoComponent? =
    (target as? PhotoTarget.Component)?.let { chosen -> components.firstOrNull { it.id == chosen.componentId } }

/** Соответствия после того, как компонент выключен или удалён (AWP-21.6): недействительные цели → умолчания. */
internal fun PhotoDictionaryForm.revalidate(mapping: PhotoMapping): PhotoMapping = resolveMapping(mapping)
