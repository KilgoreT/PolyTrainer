package me.apomazkin.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle

/**
 * Сборка экранов вкладок app-слоем. Навигационных лямбд здесь больше
 * нет: Mate-экраны выражают переходы навигационными эффектами, которые
 * довозит shared nav-handler по таблице appNavTable. Лямбды back
 * остались только у не-Mate экранов (AboutApp, WebView) — это их
 * единственное взаимодействие с навигацией.
 */
@Stable
interface CompositionRoot {
    /**
     * Вкладки «Слова | Группы». [savedStateHandle] — записи вкладки в стеке
     * навигации: через него экран лексем-кандидатов возвращает счёт
     * записанного, когда закрывается (AWP-23).
     */
    @Composable
    fun VocabularyHostDep(savedStateHandle: SavedStateHandle)

    @Composable
    fun WordCardScreenDep(wordId: Long)

    /** Экран лексем-кандидатов со снимка; [uri] — файл снимка строкой. */
    @Composable
    fun ScanCandidatesScreenDep(uri: String)

    @Composable
    fun QuizTabScreenDep()

    @Composable
    fun ChatQuizScreenDep()

    @Composable
    fun StatisticTabScreenDep()

    @Composable
    fun SettingsTabScreenDep()

    @Composable
    fun AboutAppScreenDep(onBackPress: () -> Unit)

    @Composable
    fun WebViewScreenDep(
        pageKey: String,
        onBackPress: () -> Unit,
    )

    @Composable
    fun PerDictionaryComponentsScreenDep(dictionaryId: Long)
}
