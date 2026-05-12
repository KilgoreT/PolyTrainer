package me.apomazkin.dictionarytab.ui

import me.apomazkin.mate.Navigator

interface VocabularyNavigator : Navigator {
    fun openWordCard(wordId: Long)
}
