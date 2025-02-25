package me.apomazkin.ui.resource

interface ResourceManager {
    fun stringByResId(id: Int): String
    fun stringByResId(id: Int, value: String): String
    fun stringByArrayId(id: Int): String
}