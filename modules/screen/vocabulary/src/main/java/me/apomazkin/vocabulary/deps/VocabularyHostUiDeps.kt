package me.apomazkin.vocabulary.deps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

/**
 * IS493 Э1: AppBar-слот host'а — приём `DictionaryTabUiDeps.AppBar` переехал
 * из words-модуля (D1.4); реализует app (`DictionaryAppBar` с той же проводкой).
 */
interface VocabularyHostUiDeps {
    @Composable
    fun AppBar(@StringRes titleResId: Int)
}
