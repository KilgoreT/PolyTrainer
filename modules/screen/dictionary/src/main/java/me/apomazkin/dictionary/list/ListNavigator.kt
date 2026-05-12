package me.apomazkin.dictionary.list

import me.apomazkin.mate.Navigator

interface ListNavigator : Navigator {
    fun exit()
    fun openEdit(id: Long)
    fun openCreate()
}
