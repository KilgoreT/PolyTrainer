package me.apomazkin.dictionarytab.deps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

interface DictionaryTabUiDeps {
    @Composable
    fun AppBar(@StringRes titleResId: Int)
}