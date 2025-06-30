package me.apomazkin.quiztab.deps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

interface QuizTabUiDeps {
    @Composable
    fun AppBar(@StringRes titleResId: Int)
}