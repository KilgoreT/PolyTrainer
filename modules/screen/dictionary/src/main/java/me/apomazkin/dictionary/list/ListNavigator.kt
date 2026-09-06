package me.apomazkin.dictionary.list

import io.github.kilgoret.mate.Navigator

interface ListNavigator : Navigator {
    fun exit()
    fun openEdit(id: Long)
    fun openCreate()
}
