package me.apomazkin.stattab.deps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

interface StatisticUiDeps {
    @Composable
    fun AppBar(@StringRes titleResId: Int)
}