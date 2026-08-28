package me.apomazkin.groupstab.ui

import me.apomazkin.mate.Navigator

/** IS493 Э2 (D10.4): навигация вкладки «Группы» — по образцу WordsNavigator. */
interface GroupsNavigator : Navigator {
    fun openWordCard(wordId: Long)
}
