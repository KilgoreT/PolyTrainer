package me.apomazkin.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Сборка экранов вкладок app-слоем. Навигационных лямбд здесь больше
 * нет: Mate-экраны выражают переходы навигационными эффектами, которые
 * довозит shared nav-handler по таблице appNavGraph. Лямбды back
 * остались только у не-Mate экранов (AboutApp, WebView) — это их
 * единственное взаимодействие с навигацией.
 */
@Stable
interface CompositionRoot {
    @Composable
    fun VocabularyHostDep()

    @Composable
    fun WordCardScreenDep(wordId: Long)

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
