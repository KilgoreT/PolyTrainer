package me.apomazkin.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface MainUiDeps {
    @Composable
    fun VocabularyTab(
        openAddDict: () -> Unit,
        openWordCard: (wordId: Long) -> Unit,
    )

    @Composable
    fun WordCardScreenDep(
        wordId: Long,
        onBackPress: () -> Unit,
    )
}