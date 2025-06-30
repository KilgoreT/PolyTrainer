package me.apomazkin.polytrainer.uiDeps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import me.apomazkin.dictionaryappbar.DictionaryAppBar
import me.apomazkin.dictionaryappbar.deps.DictionaryAppBarUseCase
import me.apomazkin.dictionarytab.deps.DictionaryTabUiDeps
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.ui.DictionaryTabScreen
import me.apomazkin.main.MainUiDeps
import me.apomazkin.polytrainer.env.EnvParams
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.ChatScreen
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiztab.QuizTabScreen
import me.apomazkin.quiztab.deps.QuizTabUiDeps
import me.apomazkin.quiztab.deps.QuizTabUseCase
import me.apomazkin.settingstab.AboutAppScreen
import me.apomazkin.settingstab.SettingsTabScreen
import me.apomazkin.settingstab.deps.SettingsTabUseCase
import me.apomazkin.stattab.StatisticTabScreen
import me.apomazkin.stattab.deps.StatisticUiDeps
import me.apomazkin.stattab.deps.StatisticUseCase
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
    private val statisticUseCase: StatisticUseCase,
    private val dictionaryAppBarUseCase: DictionaryAppBarUseCase,
    private val settingsTabUseCase: SettingsTabUseCase,
    private val resourceManager: ResourceManager,
    private val prefsProvider: PrefsProvider,
    private val envParams: EnvParams,
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
            openWordCard = openWordCard,
            dictionaryTabUiDeps = object : DictionaryTabUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    logger = logger,
                    dictionaryAppBarUseCase = dictionaryAppBarUseCase,
                    openAddDict = openAddDict,
                )
            },
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
            openQuiz = openChatQuiz,
            quizTabUiDeps = object : QuizTabUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    logger = logger,
                    dictionaryAppBarUseCase = dictionaryAppBarUseCase,
                    openAddDict = openAddDict,
                )
            },
        )
    }

    @Composable
    override fun ChatQuizScreenDep(
        onBackPress: () -> Unit,
    ) {
        ChatScreen(
            quizChatUseCase = quizChatUseCase,
            resourceManager = resourceManager,
            prefsProvider = prefsProvider,
            logger = logger,
            onBackPress = onBackPress,
        )
    }

    @Composable
    override fun StatisticTabScreenDep(
        openAddDict: () -> Unit,
    ) {
        StatisticTabScreen(
            statisticUseCase = statisticUseCase,
            statisticUiDeps = object : StatisticUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    logger = logger,
                    dictionaryAppBarUseCase = dictionaryAppBarUseCase,
                    openAddDict = openAddDict,
                )
            },
            logger = logger,
        )
    }

    @Composable
    override fun SettingsTabScreenDep(
        onLangManagementClick: () -> Unit,
        onAboutAppClick: () -> Unit,
    ) {
        SettingsTabScreen(
            onLangManagementClick = onLangManagementClick,
            onAboutAppClick = onAboutAppClick,
            settingsTabUseCase = settingsTabUseCase,
            logger = logger,
        )
    }

    @Composable
    override fun AboutAppScreenDep(
        onBackPress: () -> Unit,
    ) {
        AboutAppScreen(
            appVersion = envParams.appVersion,
            onBackPress = onBackPress,
        )
    }
}