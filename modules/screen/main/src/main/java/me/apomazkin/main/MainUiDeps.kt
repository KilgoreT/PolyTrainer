package me.apomazkin.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface MainUiDeps {
    @Composable
    fun VocabularyTabDep(
            openDictionaryManagement: () -> Unit,
            openWordCard: (wordId: Long) -> Unit,
    )

    @Composable
    fun WordCardScreenDep(
            wordId: Long,
            onBackPress: () -> Unit,
    )

    @Composable
    fun QuizTabScreenDep(
            openDictionaryManagement: () -> Unit,
            openChatQuiz: (quizType: String) -> Unit,
    )

    @Composable
    fun ChatQuizScreenDep(
            onBackPress: () -> Unit,
    )

    @Composable
    fun StatisticTabScreenDep(
            openDictionaryManagement: () -> Unit,
    )

    @Composable
    fun SettingsTabScreenDep(
            onLangManagementClick: () -> Unit,
            onAboutAppClick: () -> Unit,
    )

    @Composable
    fun AboutAppScreenDep(
            onBackPress: () -> Unit,
    )
}