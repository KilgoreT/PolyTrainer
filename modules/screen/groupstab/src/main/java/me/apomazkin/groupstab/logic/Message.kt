package me.apomazkin.groupstab.logic

import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.group.DeleteGroupWithWordsOutcome
import me.apomazkin.group.DisplayTree
import io.github.kilgoret.mate.Effect
import me.apomazkin.wordrow.entity.TermUiItem

sealed interface Msg {

    /**
     * Проводка словаря от host (D9.4): шлётся экраном по DictionarySlot
     * ТОЛЬКО при isResolved; null — «словарей нет».
     */
    data class DictionaryChanged(val dictionaryId: Long?) : Msg

    /** Slice пришёл/переэмитил (живая подписка, А13) — счётчик/структура. */
    data class SliceLoaded(val tree: DisplayTree) : Msg

    /** Тап по строке «Все»: раскрыть/свернуть. */
    data object ToggleAll : Msg

    /** Тап по кнопке «Ещё» — расширить окно. */
    data object LoadMore : Msg

    /** Эмиссия живого окна контента (целиком, не порция). */
    data class WindowLoaded(val words: List<TermUiItem>) : Msg

    /** Ошибка подписки окна (T-6): гасим спиннер, кнопка живая. */
    data object WindowLoadFailed : Msg

    /** Ошибка подписки slice (T-6): гасим общий loading. */
    data object SliceLoadFailed : Msg

    // === Э3 (D15.4): шторка, kebab, конфирм, мутации ===

    /** FAB → шторка создания. */
    data object OpenCreateSheet : Msg

    /** Kebab «Переименовать» → шторка с предзаполненным именем. */
    data class OpenRenameSheet(val groupId: Long) : Msg

    /** Ввод в шторке; в Create-режиме — ещё и live-фильтр списка (В1). */
    data class SheetInputChanged(val value: String) : Msg

    /** Отправка шторки (create либо rename по mode). */
    data object SubmitSheet : Msg

    data object DismissSheet : Msg

    // Итог мутации имени (create/rename): handler маппит доменный outcome
    // в один из трёх ПЛОСКИХ Msg (toMutationMsg) — простое сообщение →
    // простое изменение стейта. Какая операция ответила — в логе handler'а
    // («create/rename outcome: …») строкой выше.

    /** Мутация применена (или NotFound-гонка rename): шторку закрыть. */
    data object MutationApplied : Msg

    /** Имя отвергнуто валидацией: показать [error] (inline/снекбар). */
    data class MutationRejected(val error: GroupSheetError) : Msg

    /** Недостижимые в Э3 ветки-заделы (Э4/Э6): молча снять submit. */
    data object MutationIgnored : Msg

    /** Kebab «Удалить» → конфирм (В2: всегда). */
    data class RequestDelete(val groupId: Long) : Msg

    data object ConfirmDelete : Msg

    data object DismissDelete : Msg

    /** Э6: галка «удалить вместе со словами» в конфирме. */
    data object ToggleDeleteWords : Msg

    /** Э6: секундный тик паузы осмысления (шлёт handler). */
    data object DeleteCountdownTick : Msg

    /** Итог удаления: state не трогаем — список обновит живая подписка. */
    data class DeleteOutcomeMsg(val outcome: DeleteGroupOutcome) : Msg

    /** Э6: итог деструктивного удаления — аналогично no-op. */
    data class DeleteWithWordsOutcomeMsg(
        val outcome: DeleteGroupWithWordsOutcome,
    ) : Msg

    /** Исключение эффекта мутации (guard T-6): снять isSubmitting. */
    data object GroupMutationFailed : Msg

    /** Снекбар ошибки показан — сброс флага. */
    data object ErrorSnackbarShown : Msg

    data class OpenKebab(val groupId: Long) : Msg

    data object DismissKebab : Msg

    /** Раскрыть/свернуть обычную группу (Э5: открыть/закрыть её окно). */
    data class ToggleGroup(val groupId: Long) : Msg

    // === Э5 (D21): живые окна раскрытых групп ===

    /** Тап по слову в окне контента → эффект открытия карточки. */
    data class OpenWordCard(val wordId: Long) : Msg

    /** «Ещё» в контенте раскрытой группы. */
    data class LoadMoreGroup(val groupId: Long) : Msg

    /** Эмиссия живого окна группы (контент целиком, не порция). */
    data class GroupWindowLoaded(val groupId: Long, val words: List<TermUiItem>) : Msg

    /** Ошибка подписки окна группы: гасим спиннер, окно живо. */
    data class GroupWindowFailed(val groupId: Long) : Msg
}

sealed interface GroupsEffect : Effect {

    // Здесь только РАЗОВЫЕ намерения. Длящиеся (живой slice, окна
    // контента, тикер отсчёта) эффектами не выражаются — они
    // декларируются набором подписок subscriptions() ([GroupsSub])
    // и управляются диффом раннера.

    // === Э3: мутации (handler → use case → outcome-Msg) ===

    data class CreateGroup(val dictionaryId: Long, val name: String) : GroupsEffect

    data class RenameGroup(val groupId: Long, val name: String) : GroupsEffect

    data class DeleteGroup(val groupId: Long) : GroupsEffect

    /** Э6: ДЕСТРУКТИВ — удалить группу вместе со словами (D30.2). */
    data class DeleteGroupWithWords(val groupId: Long) : GroupsEffect
}
