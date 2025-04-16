package me.apomazkin.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

@Stable
interface MainUiDeps {
    @Composable
    fun VocabularyTabDep(
        openAddDict: () -> Unit,
        openWordCard: (wordId: Long) -> Unit,
    )
    
    @Composable
    fun WordCardScreenDep(
        wordId: Long,
        onBackPress: () -> Unit,
    )
    
    @Composable
    fun QuizTabScreenDep(
        openAddDict: () -> Unit,
        openChatQuiz: (quizType: String) -> Unit,
    )
    
    @Composable
    fun ChatQuizScreenDep(
        onBackPress: () -> Unit,
    )
    
    @Composable
    fun StatisticTabScreenDep()
    
    @Composable
    fun SettingsTabScreenDep(
        onLangManagementClick: () -> Unit,
        onAboutAppClick: () -> Unit,
    )
    
    @Composable
    fun AboutAppScreenDep(
        onBackPress: () -> Unit
    )
}