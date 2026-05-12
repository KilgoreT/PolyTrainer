package me.apomazkin.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface CompositionRoot {
    @Composable
    fun VocabularyTabDep(
            openDictionaryCreate: () -> Unit,
            openWordCard: (wordId: Long) -> Unit,
    )

    @Composable
    fun WordCardScreenDep(
            wordId: Long,
            onBackPress: () -> Unit,
    )

    @Composable
    fun QuizTabScreenDep(
            openDictionaryCreate: () -> Unit,
            openChatQuiz: (quizType: String) -> Unit,
    )

    @Composable
    fun ChatQuizScreenDep(
            onBackPress: () -> Unit,
    )

    @Composable
    fun StatisticTabScreenDep(
            openDictionaryCreate: () -> Unit,
    )

    @Composable
    fun SettingsTabScreenDep(
            onLangManagementClick: () -> Unit,
            onAboutAppClick: () -> Unit,
            onPrivacyPolicyClick: () -> Unit,
    )

    @Composable
    fun AboutAppScreenDep(
            onBackPress: () -> Unit,
    )

    @Composable
    fun WebViewScreenDep(
            pageKey: String,
            onBackPress: () -> Unit,
    )
}
