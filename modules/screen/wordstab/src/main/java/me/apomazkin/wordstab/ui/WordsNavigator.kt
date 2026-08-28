package me.apomazkin.wordstab.ui

import me.apomazkin.mate.Navigator

interface WordsNavigator : Navigator {
    fun openWordCard(wordId: Long)
}
