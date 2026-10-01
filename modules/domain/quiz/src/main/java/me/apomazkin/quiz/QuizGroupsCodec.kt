package me.apomazkin.quiz

/**
 * Сохранённый набор групп в разобранном виде: [ids] — годные токены,
 * [hasGarbage] — в сохранённой строке были токены, которые не разобрать
 * в id (или лишние разделители). Мусор не влияет на выбор, но закрепляется
 * так же, как выпавшая группа: подписка плашки перезаписывает набор
 * очищенным.
 */
data class PersistedQuizGroups(
    val ids: Set<Long>,
    val hasGarbage: Boolean,
)

private const val GROUP_IDS_SEPARATOR = ","

/** Набор → строка pref: id по возрастанию через запятую; пусто — `null` (ключ стирается). */
fun encodeQuizGroupIds(ids: Set<Long>): String? =
    if (ids.isEmpty()) null else ids.sorted().joinToString(GROUP_IDS_SEPARATOR)

/**
 * Строка pref → набор. `null` / пустая строка — пустой набор («Все»).
 * Мусор отбрасывается и помечается: строка не совпадает с каноническим
 * видом своего же разбора.
 */
fun decodeQuizGroupIds(raw: String?): PersistedQuizGroups {
    if (raw.isNullOrEmpty()) return PersistedQuizGroups(ids = emptySet(), hasGarbage = false)
    val ids = raw
        .split(GROUP_IDS_SEPARATOR)
        .mapNotNull { it.trim().toLongOrNull() }
        .toSet()
    return PersistedQuizGroups(
        ids = ids,
        hasGarbage = raw != encodeQuizGroupIds(ids),
    )
}
