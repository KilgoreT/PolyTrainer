package me.apomazkin.wordstab.ui

import io.github.kilgoret.mate.Navigator

interface WordsNavigator : Navigator {
    fun openWordCard(wordId: Long)
}
