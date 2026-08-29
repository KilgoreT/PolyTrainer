package me.apomazkin.polytrainer.navigator

import me.apomazkin.groupstab.ui.GroupsNavigator

/**
 * IS493 Э2 (D10.4): навигация вкладки «Группы» — переиспользует
 * wordcard-роут (лямбда приходит из VocabularyHostDep).
 */
class GroupsNavigatorImpl(
    private val onOpenWordCard: (wordId: Long) -> Unit,
) : GroupsNavigator {

    override fun openWordCard(wordId: Long) {
        onOpenWordCard(wordId)
    }

    // Вкладки «назад» не перехватывают (В5) — у групп нет своей back-навигации.
    override fun back() = Unit
}
