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
 * валидированный выбор (`null` = «Все»).
 */
data class QuizGroupState(
    val options: QuizGroupOptions,
    val selectedGroupId: Long?,
)

/** Вход воронки: живая группа со счётчиком уровня 1 (снапшот data-слоя). */
data class QuizGroupCount(
    val id: Long,
    val name: String,
    val wordCount: Int,
)

/**
 * Единственная точка валидации выбора группы: и selection-store
 * (снапшот для квиза), и подписка плашки строят состояние ТОЛЬКО этой
 * функцией — два пути не могут разойтись.
 *
 * Резолв персиста — молчаливый фолбэк на «Все»: [persistedGroupId],
 * не входящий в пригодное множество (удалённая, усохшая ниже порога
 * или чужая группа), даёт `null`. Фолбэк ЗАКРЕПЛЯЕТСЯ вызывающей
 * стороной (плашка стирает невалидный pref эффектом) — «Все» остаётся
 * выбором и после исцеления группы, воскрешений нет (решение прогона
 * 2026-09-26).
 */
fun resolveQuizGroupState(
    groupCounts: List<QuizGroupCount>,
    dictionaryWordCount: Int,
    persistedGroupId: Long?,
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
    val selectedGroupId = persistedGroupId?.takeIf { id ->
        groups.any { it.id == id && it.isEligible }
    }
    return QuizGroupState(
        options = options,
        selectedGroupId = selectedGroupId,
    )
}
