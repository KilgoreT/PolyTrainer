package me.apomazkin.dictionaryappbar

import io.github.kilgoret.mate.Navigator

interface DictionaryAppBarNavigator : Navigator {
    fun openDictionaryCreate()
    fun openPerDictionaryComponents(dictionaryId: Long)
}
