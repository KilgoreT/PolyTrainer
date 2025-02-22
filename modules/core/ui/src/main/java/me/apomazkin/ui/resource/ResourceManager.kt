package me.apomazkin.ui.resource

interface ResourceManager {
    fun stringByResId(id: Int): String
    fun stringByArrayId(id: Int): String
}