package me.apomazkin.polytrainer.uiDeps

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.ui.DictionaryTabScreen
import me.apomazkin.main.MainUiDeps
import me.apomazkin.quiz.chat.ChatScreen
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiztab.QuizTabScreen
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.settingstab.AboutAppScreen
import me.apomazkin.settingstab.SettingsTabScreen
import me.apomazkin.stattab.StatisticTabScreen
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.resource.ResourceManager
import me.apomazkin.wordcard.WordCardScreen
import me.apomazkin.wordcard.deps.WordCardUseCase

@Stable
class MainUiDepsProvider(
    private val dictionaryTabUseCase: DictionaryTabUseCase,
    private val wordCardUseCase: WordCardUseCase,
    private val quizTabUseCase: QuizTabUseCase,
    private val quizChatUseCase: QuizChatUseCase,
    private val resourceManager: ResourceManager,
    private val logger: LexemeLogger,
) : MainUiDeps {
    @Composable
    override fun VocabularyTabDep(
        openAddDict: () -> Unit,
        openWordCard: (wordId: Long) -> Unit,
    ) {
        DictionaryTabScreen(
            dictionaryTabUseCase = dictionaryTabUseCase,
            logger = logger,
            openAddDict = openAddDict,
            openWordCard = openWordCard,
        )
    }

    @Composable
    override fun WordCardScreenDep(
        wordId: Long,
        onBackPress: () -> Unit,
    ) {
        WordCardScreen(
            wordId = wordId,
            wordCardUseCase = wordCardUseCase,
            onBackPress = onBackPress,
        )
    }

    @Composable
    override fun QuizTabScreenDep(
        openAddDict: () -> Unit,
        openChatQuiz: (quizType: String) -> Unit,
    ) {
        QuizTabScreen(
            quizTabUseCase = quizTabUseCase,
            logger = logger,
            openAddDict = openAddDict,
            openQuiz = openChatQuiz,
        )
    }
    
    @Composable
    override fun ChatQuizScreenDep(
        onBackPress: () -> Unit,
    ) {
        ChatScreen(
            quizChatUseCase = quizChatUseCase,
            resourceManager = resourceManager,
            logger = logger,
            onBackPress = onBackPress,
        )
    }

    @Composable
    override fun StatisticTabScreenDep() {
        StatisticTabScreen()
    }

    @Composable
    override fun SettingsTabScreenDep(
        onLangManagementClick: () -> Unit,
        onAboutAppClick: () -> Unit,
    ) {
        SettingsTabScreen(
            onLangManagementClick = onLangManagementClick,
            onAboutAppClick = onAboutAppClick,
        )
    }
    
    @Composable
    override fun AboutAppScreenDep(
        onBackPress: () -> Unit,
    ) {
        AboutAppScreen(
            onBackPress = onBackPress,
        )
    }
}