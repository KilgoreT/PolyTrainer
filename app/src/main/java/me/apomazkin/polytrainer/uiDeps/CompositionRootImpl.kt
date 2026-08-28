package me.apomazkin.polytrainer.uiDeps

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import me.apomazkin.dictionaryappbar.DictionaryAppBar
import me.apomazkin.dictionaryappbar.DictionaryAppBarViewModel
import me.apomazkin.wordstab.ui.WordsTabViewModel
import me.apomazkin.wordstab.ui.rememberWordsTabHandle
import me.apomazkin.groupstab.ui.GroupsTabViewModel
import me.apomazkin.groupstab.ui.rememberGroupsTabHandle
import me.apomazkin.polytrainer.navigator.GroupsNavigatorImpl
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.main.CompositionRoot
import me.apomazkin.per_dictionary_components.PerDictionaryComponentsScreen
import me.apomazkin.per_dictionary_components.PerDictionaryComponentsViewModel
import me.apomazkin.polytrainer.LogTags
import me.apomazkin.polytrainer.env.EnvParams
import me.apomazkin.polytrainer.navigator.ChatNavigatorImpl
import me.apomazkin.polytrainer.navigator.DictionaryAppBarNavigatorImpl
import me.apomazkin.polytrainer.navigator.PerDictionaryComponentsNavigatorImpl
import me.apomazkin.polytrainer.navigator.QuizTabNavigatorImpl
import me.apomazkin.polytrainer.navigator.SettingsNavigatorImpl
import me.apomazkin.polytrainer.navigator.StatisticNavigatorImpl
import me.apomazkin.polytrainer.navigator.WordsNavigatorImpl
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
import me.apomazkin.vocabulary.FabSpec
import me.apomazkin.vocabulary.TabSpec
import me.apomazkin.vocabulary.VocabularyTab
import me.apomazkin.vocabulary.deps.VocabularyHostUiDeps
import me.apomazkin.vocabulary.ui.VocabularyHostScreen
import me.apomazkin.vocabulary.ui.VocabularyHostViewModel
import me.apomazkin.wordcard.WordCardScreen
import me.apomazkin.wordcard.WordCardViewModel

@Stable
class CompositionRootImpl(
    private val wordCardViewModelFactory: WordCardViewModel.Factory,
    private val chatViewModelFactory: ChatViewModel.Factory,
    private val appBarViewModelFactory: DictionaryAppBarViewModel.Factory,
    private val wordstabViewModelFactory: WordsTabViewModel.Factory,
    private val vocabularyHostViewModelFactory: VocabularyHostViewModel.Factory,
    private val groupsTabViewModelFactory: GroupsTabViewModel.Factory,
    private val quizTabViewModelFactory: QuizTabViewModel.Factory,
    private val statisticViewModelFactory: StatisticViewModel.Factory,
    private val settingsTabViewModelFactory: SettingsTabViewModel.Factory,
    private val perDictionaryComponentsViewModelFactory: PerDictionaryComponentsViewModel.Factory,
    private val envParams: EnvParams,
    private val logger: LexemeLogger,
) : CompositionRoot {
    /**
     * IS493 Э1: host вкладок «Слова | Группы». Мост — чистая склейка
     * (stage1_design_tree D4): words-VM живёт внутри модуля за
     * [rememberWordsTabHandle], здесь только сборка TabSpec'ов в remember.
     */
    @Composable
    override fun VocabularyHostDep(
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
            WordsNavigatorImpl(onOpenWordCard = openWordCard)
        }
        val groupsNavigator = remember(openWordCard) {
            GroupsNavigatorImpl(onOpenWordCard = openWordCard)
        }
        val words = rememberWordsTabHandle(
            factory = wordstabViewModelFactory,
            navigator = vocabularyNavigator,
        )
        // IS493 Э3 (D16): handle групп — по образцу words; VM живёт внутри
        // модуля, мост получает FAB-читалки и Content.
        val groups = rememberGroupsTabHandle(
            factory = groupsTabViewModelFactory,
            navigator = groupsNavigator,
        )
        // Спеки СТАБИЛЬНЫ (remember только по handle): пересоздание TabSpec в
        // кадр открытия шторки рвало её show-анимацию (баг Э1 — невидимый
        // ModalBottomSheet). Изменчивость — только через лямбды-читалки,
        // которые host вызывает в своих скоупах. Словарь приходит в content
        // параметром вызова (DictionarySlot, D9.4) — спеки не пересобираются.
        val tabs = remember(words, groups) {
            mapOf(
                VocabularyTab.WORDS to TabSpec(
                    titleRes = me.apomazkin.core_resources.R.string.vocabulary_tab_words,
                    isTopBarOverridden = { words.isActionMode.value },
                    topBarOverride = { words.ActionTopBar() },
                    fab = FabSpec(
                        iconRes = words.fabIconRes,
                        visible = { words.isFabVisible.value },
                        onClick = words::onFabClick,
                    ),
                    // words словарь-слот игнорирует — резолвит сам (В3/D9.2).
                    content = { snackbarHostState, _ -> words.Content(snackbarHostState) },
                ),
                VocabularyTab.GROUPS to TabSpec(
                    titleRes = me.apomazkin.core_resources.R.string.vocabulary_tab_groups,
                    // Э3: FAB создания группы; скрыт под шторкой/конфирмом
                    // (читалка — урок D1.3). Drawable живёт в core-resources
                    // (F-6). Мост распаковывает DictionarySlot в примитивы —
                    // groupstab контракта host'а не видит.
                    fab = FabSpec(
                        iconRes = me.apomazkin.core_resources.R.drawable.ic_add,
                        visible = { groups.isFabVisible.value },
                        onClick = groups::onFabClick,
                    ),
                    content = { snackbarHostState, slot ->
                        groups.Content(
                            snackbarHostState = snackbarHostState,
                            dictionaryId = slot.id,
                            isDictResolved = slot.isResolved,
                        )
                    },
                ),
            )
        }
        VocabularyHostScreen(
            tabs = tabs,
            factory = vocabularyHostViewModelFactory,
            uiDeps = object : VocabularyHostUiDeps {
                @Composable
                override fun AppBar(@StringRes titleResId: Int) = DictionaryAppBar(
                    titleResId = titleResId,
                    factory = appBarViewModelFactory,
                    navigator = appBarNavigator,
                )
            },
            onTabSwitched = { words.onExitSelectionMode() },
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
    ) {
        val navigator = remember(
            onLangManagementClick,
            onAboutAppClick,
            onPrivacyPolicyClick,
        ) {
            SettingsNavigatorImpl(
                onOpenLangManagement = onLangManagementClick,
                onOpenAboutApp = onAboutAppClick,
                onOpenWebView = { pageKey ->
                    logger.log(tag = me.apomazkin.settingstab.LogTags.SETTINGS, message = "navigate: $pageKey")
                    onPrivacyPolicyClick()
                },
            )
        }
        SettingsTabScreen(
            factory = settingsTabViewModelFactory,
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
