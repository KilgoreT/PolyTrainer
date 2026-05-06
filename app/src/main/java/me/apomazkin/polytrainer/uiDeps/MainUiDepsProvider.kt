package me.apomazkin.polytrainer.uiDeps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.res.stringResource
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
import me.apomazkin.settingstab.WebViewScreen
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
        openDictionaryCreate: () -> Unit,
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
                    openDictionaryCreate = openDictionaryCreate,
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
        openDictionaryCreate: () -> Unit,
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
                    openDictionaryCreate = openDictionaryCreate,
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
        openDictionaryCreate: () -> Unit,
    ) {
        StatisticTabScreen(
            statisticUseCase = statisticUseCase,
            statisticUiDeps = object : StatisticUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    logger = logger,
                    dictionaryAppBarUseCase = dictionaryAppBarUseCase,
                    openDictionaryCreate = openDictionaryCreate,
                )
            },
            logger = logger,
        )
    }

    @Composable
    override fun SettingsTabScreenDep(
        onLangManagementClick: () -> Unit,
        onAboutAppClick: () -> Unit,
        onPrivacyPolicyClick: () -> Unit,
    ) {
        SettingsTabScreen(
            onLangManagementClick = onLangManagementClick,
            onAboutAppClick = onAboutAppClick,
            onPrivacyPolicyClick = {
                logger.log(tag = "###Settings###", message = "navigate: privacy_policy")
                onPrivacyPolicyClick()
            },
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

    @Composable
    override fun WebViewScreenDep(
        pageKey: String,
        onBackPress: () -> Unit,
    ) {
        val webPage = WebPage.fromKey(pageKey) ?: run {
            logger.log(tag = "###WebView###", message = "unknown pageKey: $pageKey")
            return
        }
        WebViewScreen(
            url = webPage.url,
            title = stringResource(id = webPage.titleRes),
            pageKey = pageKey,
            logger = logger,
            onBackPress = onBackPress,
        )
    }
}

private enum class WebPage(val key: String, val url: String, @StringRes val titleRes: Int) {
    PRIVACY_POLICY(
        key = "privacy_policy",
        url = "https://kilgoret.github.io/lexeme-docs/privacy-policy",
        titleRes = me.apomazkin.core_resources.R.string.settings_section_privacy_policy,
    );

    companion object {
        fun fromKey(key: String): WebPage? = entries.find { it.key == key }
    }
}