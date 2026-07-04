package me.apomazkin.dictionaryappbar

import me.apomazkin.mate.Navigator

interface DictionaryAppBarNavigator : Navigator {
    fun openDictionaryCreate()
    fun openPerDictionaryComponents(dictionaryId: Long)
}
