package me.apomazkin.polytrainer.uiDeps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import me.apomazkin.components_manager.ComponentsManagerScreen
import me.apomazkin.components_manager.ComponentsManagerViewModel
import me.apomazkin.dictionaryappbar.DictionaryAppBar
import me.apomazkin.dictionaryappbar.DictionaryAppBarViewModel
import me.apomazkin.dictionarytab.deps.DictionaryTabUiDeps
import me.apomazkin.dictionarytab.ui.DictionaryTabScreen
import me.apomazkin.dictionarytab.ui.DictionaryTabViewModel
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.main.CompositionRoot
import me.apomazkin.per_dictionary_components.PerDictionaryComponentsScreen
import me.apomazkin.per_dictionary_components.PerDictionaryComponentsViewModel
import me.apomazkin.polytrainer.LogTags
import me.apomazkin.polytrainer.env.EnvParams
import me.apomazkin.polytrainer.navigator.ChatNavigatorImpl
import me.apomazkin.polytrainer.navigator.ComponentsManagerNavigatorImpl
import me.apomazkin.polytrainer.navigator.DictionaryAppBarNavigatorImpl
import me.apomazkin.polytrainer.navigator.PerDictionaryComponentsNavigatorImpl
import me.apomazkin.polytrainer.navigator.QuizTabNavigatorImpl
import me.apomazkin.polytrainer.navigator.SettingsNavigatorImpl
import me.apomazkin.polytrainer.navigator.StatisticNavigatorImpl
import me.apomazkin.polytrainer.navigator.VocabularyNavigatorImpl
import me.apomazkin.polytrainer.navigator.WordCardNavigatorImpl
import me.apomazkin.quiz.chat.ChatScreen
import me.apomazkin.quiz.chat.ChatViewModel
import me.apomazkin.quiztab.QuizTabScreen
import me.apomazkin.quiztab.QuizTabViewModel
import me.apomazkin.quiztab.deps.QuizTabUiDeps
import me.apomazkin.settingstab.AboutAppScreen
import me.apomazkin.settingstab.SettingsTabScreen
import me.apomazkin.settingstab.SettingsTabViewModel
import me.apomazkin.settingstab.WebViewScreen
import me.apomazkin.stattab.StatisticTabScreen
import me.apomazkin.stattab.StatisticViewModel
import me.apomazkin.stattab.deps.StatisticUiDeps
import me.apomazkin.wordcard.WordCardScreen
import me.apomazkin.wordcard.WordCardViewModel

@Stable
class CompositionRootImpl(
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    private val chatViewModelFactory: ChatViewModel.Factory,
    private val appBarViewModelFactory: DictionaryAppBarViewModel.Factory,
    private val dictionaryTabViewModelFactory: DictionaryTabViewModel.Factory,
    private val quizTabViewModelFactory: QuizTabViewModel.Factory,
    private val statisticViewModelFactory: StatisticViewModel.Factory,
    private val settingsTabViewModelFactory: SettingsTabViewModel.Factory,
    private val componentsManagerViewModelFactory: ComponentsManagerViewModel.Factory,
    private val perDictionaryComponentsViewModelFactory: PerDictionaryComponentsViewModel.Factory,
    private val envParams: EnvParams,
    private val logger: LexemeLogger,
) : CompositionRoot {
    @Composable
    override fun VocabularyTabDep(
        openDictionaryCreate: () -> Unit,
        openWordCard: (wordId: Long) -> Unit,
        openPerDictionaryComponents: (dictionaryId: Long) -> Unit,
    ) {
        val appBarNavigator = remember(openDictionaryCreate, openPerDictionaryComponents) {
            DictionaryAppBarNavigatorImpl(
                onOpenDictionaryCreate = openDictionaryCreate,
                onOpenPerDictionaryComponents = openPerDictionaryComponents,
            )
        }
        val vocabularyNavigator = remember(openWordCard) {
            VocabularyNavigatorImpl(onOpenWordCard = openWordCard)
        }
        DictionaryTabScreen(
            factory = dictionaryTabViewModelFactory,
            navigator = vocabularyNavigator,
            dictionaryTabUiDeps = object : DictionaryTabUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    factory = appBarViewModelFactory,
                    navigator = appBarNavigator,
                )
            },
        )
    }

    @Composable
    override fun WordCardScreenDep(
        wordId: Long,
        onBackPress: () -> Unit,
    ) {
        val navigator = remember(onBackPress) { WordCardNavigatorImpl(onBack = onBackPress) }
        WordCardScreen(
            wordId = wordId,
            factory = wordCardViewModelFactory,
            navigator = navigator,
        )
    }

    @Composable
    override fun QuizTabScreenDep(
        openDictionaryCreate: () -> Unit,
        openChatQuiz: (quizType: String) -> Unit,
        openPerDictionaryComponents: (dictionaryId: Long) -> Unit,
    ) {
        val appBarNavigator = remember(openDictionaryCreate, openPerDictionaryComponents) {
            DictionaryAppBarNavigatorImpl(
                onOpenDictionaryCreate = openDictionaryCreate,
                onOpenPerDictionaryComponents = openPerDictionaryComponents,
            )
        }
        val quizTabNavigator = remember(openChatQuiz) {
            QuizTabNavigatorImpl(onOpenChat = openChatQuiz)
        }
        QuizTabScreen(
            factory = quizTabViewModelFactory,
            navigator = quizTabNavigator,
            quizTabUiDeps = object : QuizTabUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    factory = appBarViewModelFactory,
                    navigator = appBarNavigator,
                )
            },
        )
    }

    @Composable
    override fun ChatQuizScreenDep(
        onBackPress: () -> Unit,
    ) {
        val navigator = remember(onBackPress) { ChatNavigatorImpl(onBack = onBackPress) }
        ChatScreen(
            factory = chatViewModelFactory,
            navigator = navigator,
        )
    }

    @Composable
    override fun StatisticTabScreenDep(
        openDictionaryCreate: () -> Unit,
        openPerDictionaryComponents: (dictionaryId: Long) -> Unit,
    ) {
        val appBarNavigator = remember(openDictionaryCreate, openPerDictionaryComponents) {
            DictionaryAppBarNavigatorImpl(
                onOpenDictionaryCreate = openDictionaryCreate,
                onOpenPerDictionaryComponents = openPerDictionaryComponents,
            )
        }
        val statisticNavigator = remember { StatisticNavigatorImpl() }
        StatisticTabScreen(
            factory = statisticViewModelFactory,
            navigator = statisticNavigator,
            statisticUiDeps = object : StatisticUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    factory = appBarViewModelFactory,
                    navigator = appBarNavigator,
                )
            },
        )
    }

    @Composable
    override fun SettingsTabScreenDep(
        onLangManagementClick: () -> Unit,
        onAboutAppClick: () -> Unit,
        onPrivacyPolicyClick: () -> Unit,
        onComponentsManagerClick: () -> Unit,
    ) {
        val navigator = remember(
            onLangManagementClick,
            onAboutAppClick,
            onPrivacyPolicyClick,
            onComponentsManagerClick,
        ) {
            SettingsNavigatorImpl(
                onOpenLangManagement = onLangManagementClick,
                onOpenAboutApp = onAboutAppClick,
                onOpenWebView = { pageKey ->
                    logger.log(tag = me.apomazkin.settingstab.LogTags.SETTINGS, message = "navigate: $pageKey")
                    onPrivacyPolicyClick()
                },
                onOpenComponentsManager = onComponentsManagerClick,
            )
        }
        SettingsTabScreen(
            factory = settingsTabViewModelFactory,
            navigator = navigator,
        )
    }

    @Composable
    override fun ComponentsManagerScreenDep(
        onBackPress: () -> Unit,
    ) {
        val navigator = remember(onBackPress) {
            ComponentsManagerNavigatorImpl(onBack = onBackPress)
        }
        ComponentsManagerScreen(
            factory = componentsManagerViewModelFactory,
            navigator = navigator,
        )
    }

    @Composable
    override fun PerDictionaryComponentsScreenDep(
        dictionaryId: Long,
        onBackPress: () -> Unit,
    ) {
        val navigator = remember(onBackPress) {
            PerDictionaryComponentsNavigatorImpl(onBack = onBackPress)
        }
        PerDictionaryComponentsScreen(
            dictionaryId = dictionaryId,
            factory = perDictionaryComponentsViewModelFactory,
            navigator = navigator,
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
            logger.log(tag = LogTags.APP, message = "unknown pageKey: $pageKey")
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
