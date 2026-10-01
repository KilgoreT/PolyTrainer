package me.apomazkin.quiz

/**
 * Порог годности к тренировке: минимум слов (уровня 1 — с хотя бы
 * одной лексемой), при котором пункт выбора пригоден. Один на все
 * типы квизов; слой выборки принимает его параметром с этим дефолтом —
 * задел под per-тип override без перестройки слоя.
 */
const val MIN_QUIZ_WORDS = 3

/**
 * Идентификаторы типов квизов — ось ключа персиста выбора группы
 * (тип × словарь). НЕ путать с quiz config mode (`"write"`): mode
 * описывает, ЧТО спрашивать, тип квиза — КАКАЯ карточка таба.
 */
object QuizTypes {
    const val CHAT = "chat"
}

/**
 * Группа словаря в пикере квиза.
 *
 * [wordCount] — счётчик уровня 1 (слова с хотя бы одной лексемой);
 * [isEligible] — прошла порог; непригодные группы из списка НЕ
 * выбрасываются — пикер показывает их disabled-пунктами, порог
 * самообъясняющий.
 */
data class QuizGroup(
    val id: Long,
    val name: String,
    val wordCount: Int,
    val isEligible: Boolean,
)

/**
 * Пункты пикера группы. «Все» — виртуальный пункт (не группа БД):
 * [isAllEligible]/[allWordCount] описывают его отдельно; порог
 * действует и на него (весь словарь).
 */
data class QuizGroupOptions(
    val isAllEligible: Boolean,
    val allWordCount: Int,
    val groups: List<QuizGroup>,
) {
    /** Карточка квиза кликабельна, пока есть хоть один пригодный пункт. */
    val hasEligibleOption: Boolean
        get() = isAllEligible || groups.any { it.isEligible }
}

/**
 * Результат воронки [resolveQuizGroupState]: пункты пикера + уже
 * валидированный набор выбранных групп (пусто = «Все» — весь словарь,
 * включая слова без группы).
 */
data class QuizGroupState(
    val options: QuizGroupOptions,
    val selectedGroupIds: Set<Long>,
)

/** Вход воронки: живая группа со счётчиком уровня 1 (снапшот data-слоя). */
data class QuizGroupCount(
    val id: Long,
    val name: String,
    val wordCount: Int,
)

/**
 * Единственная точка валидации выбора групп: и selection-store
 * (снапшот для квиза), и подписка плашки строят состояние ТОЛЬКО этой
 * функцией — два пути не могут разойтись.
 *
 * Резолв персиста — поэлементный молчаливый фолбэк: из
 * [persistedGroupIds] остаются только пригодные группы; удалённые,
 * усохшие ниже порога и чужие выпадают. Не осталось ни одной — пусто,
 * то есть «Все». Очищенный набор ЗАКРЕПЛЯЕТСЯ вызывающей стороной
 * (плашка пишет его в pref эффектом) — выпавшая группа не возвращается
 * сама и после исцеления.
 *
 * Порядок групп — порядок [groupCounts]: сортирует вызывающая сторона
 * (locale-aware Collator), домен порядок не навязывает.
 */
fun resolveQuizGroupState(
    groupCounts: List<QuizGroupCount>,
    dictionaryWordCount: Int,
    persistedGroupIds: Set<Long>,
    threshold: Int = MIN_QUIZ_WORDS,
): QuizGroupState {
    val groups = groupCounts.map {
        QuizGroup(
            id = it.id,
            name = it.name,
            wordCount = it.wordCount,
            isEligible = it.wordCount >= threshold,
        )
    }
    val options = QuizGroupOptions(
        isAllEligible = dictionaryWordCount >= threshold,
        allWordCount = dictionaryWordCount,
        groups = groups,
    )
    val eligibleIds = groups
        .filter { it.isEligible }
        .map { it.id }
        .toSet()
    return QuizGroupState(
        options = options,
        selectedGroupIds = persistedGroupIds intersect eligibleIds,
    )
}

/**
 * Подпись выбора групп: [first] — имя первой выбранной группы в порядке
 * списка, [more] — сколько выбрано ещё («Быт +2»). Одна для карточки
 * квиза и сабтайтла чата.
 */
data class QuizGroupLabel(
    val first: String,
    val more: Int,
)

/**
 * Подпись набора [selectedIds] по порядку [groups] (уже отсортированы
 * вызывающей стороной). Ни одна выбранная не найдена в списке — `null`,
 * то есть «Все». Имена групп не уникальны, поэтому сопоставление по id.
 */
fun quizGroupLabel(
    groups: List<QuizGroup>,
    selectedIds: Set<Long>,
): QuizGroupLabel? {
    val selected = groups.filter { it.id in selectedIds }
    val first = selected.firstOrNull() ?: return null
    return QuizGroupLabel(
        first = first.name,
        more = selected.size - 1,
    )
}
