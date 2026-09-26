package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.Subscription

/**
 * Семейство подписок вкладки «Группы» — длящиеся источники данных,
 * которые экран слушает, пока они нужны его состоянию.
 *
 * Место в цикле mate: reducer меняет state → раннер вызывает
 * [subscriptions] и диффит набор с активным → исчезнувшие подписки
 * гасятся, новые уходят в [GroupsSubHandler.flow] и начинают слать Msg.
 * Императивных «подписаться/отписаться»-эффектов нет — подписка
 * объявляется данными, её жизнью управляет дифф.
 *
 * Подписка — только данные; идентичность = equality: изменился
 * параметр (например, limit окна) — это другая подписка, старая
 * гаснет, новая стартует.
 */
sealed interface GroupsSub : Subscription {
    /**
     * Текущий выбранный словарь приложения. Без параметров: вкладка
     * слушает глобальный выбор всё время жизни экрана; каждая эмиссия
     * (включая null — «словарей нет») → [Msg.DictionaryChanged].
     * Проводки словаря от host'а через UI больше нет — вкладка
     * подписана на базу сама, как «Слова».
     */
    data object CurrentDict : GroupsSub

    /**
     * Живой срез структуры словаря: membership слов + дерево групп,
     * собирается в DisplayTree. Жив всегда, пока выбран словарь.
     *
     * @param dictionaryId словарь, чью структуру слушаем.
     */
    data class Slice(
        val dictionaryId: Long,
    ) : GroupsSub

    /**
     * Живое окно контента узла «Все»: первые [limit] слов словаря.
     * Жив, пока узел раскрыт и окно непустое.
     *
     * @param dictionaryId словарь-источник слов.
     * @param limit размер окна от головы списка; рост окна («Ещё») —
     *   это новая подписка с бо́льшим limit.
     */
    data class AllWindow(
        val dictionaryId: Long,
        val limit: Int,
    ) : GroupsSub

    /**
     * Живое окно контента раскрытой группы — по одной подписке на
     * каждую раскрытую группу с непустым окном.
     *
     * @param groupId группа-источник.
     * @param limit размер окна от головы списка группы.
     */
    data class GroupWindow(
        val groupId: Long,
        val limit: Int,
    ) : GroupsSub

    /**
     * Секундный тикер паузы осмысления деструктивного удаления:
     * шлёт [Msg.DeleteCountdownTick], пока в конфирме стоит галка
     * «удалить вместе со словами» и счётчик не дошёл до нуля.
     * Снятие галки / закрытие конфирма убирает подписку из набора —
     * тикер гаснет диффом, отдельной команды «остановить» нет.
     *
     * @param groupId группа, для которой идёт отсчёт (различает
     *   тикеры при смене цели удаления).
     */
    data class DeleteCountdown(
        val groupId: Long,
    ) : GroupsSub
}

/**
 * Декларация «что вкладка слушает и при каких условиях» — единственное
 * место с этими правилами. Чистая функция от state: раннер вызывает её
 * после каждого reduce и диффом набора включает/гасит подписки.
 *
 * Правила вывода:
 * - [GroupsSub.CurrentDict] — безусловно, всё время жизни экрана;
 * - нет словаря → остальных подписок нет;
 * - [GroupsSub.Slice] — всегда при выбранном словаре;
 * - [GroupsSub.AllWindow] — узел «Все» раскрыт и window > 0;
 * - [GroupsSub.GroupWindow] — на каждую раскрытую группу с window > 0;
 * - [GroupsSub.DeleteCountdown] — конфирм деструктива с галкой и
 *   не истёкшим счётчиком.
 */
fun GroupsTabState.subscriptions(): Set<Subscription> =
    buildSet {
        add(GroupsSub.CurrentDict)
        val dictId = dictionaryId ?: return@buildSet
        add(GroupsSub.Slice(dictionaryId = dictId))
        allNode
            ?.takeIf { it.isExpanded && it.window > 0 }
            ?.let { add(GroupsSub.AllWindow(dictionaryId = dictId, limit = it.window)) }
        expandedGroupWindows.forEach { (groupId, windowState) ->
            if (windowState.window > 0) {
                add(GroupsSub.GroupWindow(groupId = groupId, limit = windowState.window))
            }
        }
        confirmDelete
            ?.takeIf { it.deleteWords && it.countdownLeft > 0 }
            ?.let { add(GroupsSub.DeleteCountdown(groupId = it.groupId)) }
    }
