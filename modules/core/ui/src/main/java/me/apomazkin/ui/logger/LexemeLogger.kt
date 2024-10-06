package me.apomazkin.ui.logger

interface LexemeLogger {
    fun log(tag: String = "##MATE##", message: String)
}