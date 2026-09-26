package me.apomazkin.groupstab.logic

import androidx.compose.runtime.Immutable
import me.apomazkin.wordrow.entity.TermUiItem

/**
 * IS493 Э2/Э3 (D10 v3, D15): state вкладки «Группы». Все флаги явные
 * (explicit state flags). Атомарные экстеншны — [StateAtoms]
 * (member-экстеншны с логированием шагов).
 */
@Immutable
data class GroupsTabState(
    val isLoading: Boolean = true,
    /** Честное «словарей нет» (resolved null от host, IS476-паттерн). */
    val hasNoDictionary: Boolean = false,
    val dictionaryId: Long? = null,
    /** Узел «Все»; null до первого slice. */
    val allNode: AllNodeState? = null,
    // === Э3 (D15.4) ===
    /** Живые группы словаря, отсортированы (из DisplayTree). */
    val groups: List<GroupUiItem> = emptyList(),
    /** Видимые строки: [groups] под активным фильтром шторки. */
    val visibleGroups: List<GroupUiItem> = emptyList(),
    /** Шторка создания/переименования; null — закрыта. */
    val sheet: GroupSheetState? = null,
    /**
     * Ошибка мутации на показ снекбаром — ТОЛЬКО путь гонки dismiss
     * (шторка уже закрыта); при открытой шторке ошибка — inline
     * ([GroupSheetState.error]). null — нет показа.
     */
    val errorSnackbar: GroupSheetError? = null,
    /** Конфирм удаления (Э6: двухстадийный объект); null — закрыт. */
    val confirmDelete: ConfirmDeleteState? = null,
    /** Открытое kebab-меню строки; null — закрыто (U-2/F-3). */
    val openMenuGroupId: Long? = null,
    /**
     * Окна раскрытых групп (Э5, D21.1): ключ в карте = группа раскрыта;
     * `window == 0` — раскрыта пустой (заглушка, подписки нет).
     */
    val expandedGroupWindows: Map<Long, GroupWindowState> = emptyMap(),
)

/**
 * IS493 Э6 (D31.1, финал 2026-08-28 — ЕДИНЫЙ диалог, переспрос
 * заменён паузой осмысления): конфирм удаления группы.
 */
@Immutable
data class ConfirmDeleteState(
    val groupId: Long,
    /** Галка «удалить вместе со словами» (по умолчанию снята):
     * кнопка становится деструктивной «Удалить всё». */
    val deleteWords: Boolean = false,
    /**
     * Пауза осмысления деструктива: секунд до активации кнопки
     * («Удалить всё (N)»); 0 — кнопка активна. Тикает effect handler
     * (решение юзера: state+эффекты, не LaunchedEffect в UI).
     */
    val countdownLeft: Int = 0,
)

/**
 * Живое окно контента раскрытой группы (Э5) — зеркало [AllNodeState]
 * без счётчика (count группы живёт в [GroupUiItem.count]).
 */
@Immutable
data class GroupWindowState(
    val window: Int,
    val loadedWords: List<TermUiItem> = emptyList(),
    val isLoading: Boolean = true,
    val hasMore: Boolean = false,
)

/**
 * Узел «Все» (единственный в Э2/Э3). Контент — ЖИВОЕ ОКНО: предикатный
 * live-запрос первых [window] слов словаря; вставки/правки/удаления
 * переэмичиваются Room'ом сами; «Ещё» расширяет [window].
 */
@Immutable
data class AllNodeState(
    val count: Int,
    val isExpanded: Boolean = false,
    val window: Int = 0,
    val loadedWords: List<TermUiItem> = emptyList(),
    val isWindowLoading: Boolean = false,
    val hasMore: Boolean = false,
)

/** Строка группы в списке (счётчик — subtreeWordCount из DisplayTree). */
@Immutable
data class GroupUiItem(
    val id: Long,
    val name: String,
    val count: Int,
)

@Immutable
sealed interface GroupSheetMode {
    data object Create : GroupSheetMode

    data class Rename(
        val groupId: Long,
    ) : GroupSheetMode
}

/** Ошибка валидации мутации имени группы. */
enum class GroupSheetError { EMPTY, DUPLICATE, RESERVED }

@Immutable
data class GroupSheetState(
    val mode: GroupSheetMode,
    val input: String = "",
    /** Ошибка валидации — строка ПОД полем шторки (снекбар под шторкой
     * и клавиатурой не виден — ручной прогон Э3). Сбрасывается вводом. */
    val error: GroupSheetError? = null,
    /** Запись летит: submit-кнопка задизейблена (ignore-while-committing). */
    val isSubmitting: Boolean = false,
)
