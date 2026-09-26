package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.NavigationEffect

/** Навигация вкладки «Группы». */
sealed interface GroupsNavigationEffect : NavigationEffect {
    /** Тап по слову в окне контента → карточка слова. */
    data class OpenWordCard(
        val wordId: Long,
    ) : GroupsNavigationEffect
}
