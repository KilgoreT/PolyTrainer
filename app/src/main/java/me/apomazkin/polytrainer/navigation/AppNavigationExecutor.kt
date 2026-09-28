package me.apomazkin.polytrainer.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import io.github.kilgoret.mate.navigation.NavCommand
import io.github.kilgoret.mate.navigation.NavigationExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.main.MainRoutes
import me.apomazkin.polytrainer.LogTags
import me.apomazkin.polytrainer.route.MainPoint
import me.apomazkin.polytrainer.route.RootPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ЕДИНСТВЕННОЕ место приложения, знающее, что такое экран: переводит
 * [NavCommand] с [AppScreen]-данными в вызовы NavController'ов.
 * Синглтон — living-цель для одного shared nav-handler'а.
 *
 * Контроллеры рождаются в композиции и живут короче синглтона, поэтому
 * привязываются late-bound ([bind]/[unbind]); готовность исполнять =
 * контроллеры привязаны И активити в STARTED ([onHostStarted]) —
 * это и есть гейт [readiness] nav-handler'а: переход в закрытый гейт
 * не исполняется и не теряется, а ждёт в очереди handler'а.
 *
 * Экранные политики вхождения в стек (какой контроллер, popUpTo,
 * singleTop, finish) собраны здесь же, по одной ветке на экран.
 */
@Singleton
class AppNavigationExecutor @Inject constructor(
    private val logger: LexemeLogger,
) : NavigationExecutor {

    private val _readiness = MutableStateFlow(false)

    /** Гейт готовности для MateNavigationHandler. */
    val readiness: StateFlow<Boolean> = _readiness.asStateFlow()

    private var rootNavController: NavHostController? = null
    private var tabsNavController: NavHostController? = null
    private var finishApp: (() -> Unit)? = null
    private var hostStarted = false

    /** Привязать living-цели из композиции RootRouter'а. */
    fun bind(
        rootNavController: NavHostController,
        tabsNavController: NavHostController,
        finishApp: () -> Unit,
    ) {
        this.rootNavController = rootNavController
        this.tabsNavController = tabsNavController
        this.finishApp = finishApp
        recomputeReadiness()
    }

    /** Отвязать цели (композиция умерла); очередь переходов живёт. */
    fun unbind() {
        rootNavController = null
        tabsNavController = null
        finishApp = null
        recomputeReadiness()
    }

    /** Lifecycle-гейт хоста: navigate() легален только в STARTED. */
    fun onHostStarted(started: Boolean) {
        hostStarted = started
        recomputeReadiness()
    }

    private fun recomputeReadiness() {
        _readiness.value = hostStarted && rootNavController != null
    }

    override fun execute(command: NavCommand) {
        logger.d(tag = LogTags.NAV, message = "execute: $command")
        // IS502 (крэш A, последний рубеж): гонки NavController
        // (`State must be at least 'CREATED'…`) — IllegalStateException.
        // Навигация — UI-жест: потерянный повторный переход безвреден,
        // упавший процесс — нет. ERROR уезжает в Crashlytics non-fatal
        // через CrashlyticsSink.
        try {
            when (command) {
                is NavCommand.Push -> push(command.screen as AppScreen)
                NavCommand.Pop -> pop()
            }
        } catch (e: IllegalStateException) {
            logger.e(
                tag = LogTags.NAV,
                message = "execute failed | command=$command",
                throwable = e,
            )
        }
    }

    /**
     * IS502 (крэш A): идемпотентный гейт двойной навигации. Повторный
     * push экрана, чей entry уже на вершине стека, но ещё не прогрет
     * до RESUMED (переход в полёте — двойной тап, дубль эффекта),
     * пропускается: второй navigate в это окно переводит
     * INITIALIZED-entry в DESTROYED и роняет процесс. Экраны
     * различаются первым сегментом route (аргументы и query
     * шаблона/значения не сравнимы строково).
     */
    private fun NavHostController.navigateGated(
        route: String,
        builder: NavOptionsBuilder.() -> Unit = {},
    ) {
        val top = currentBackStackEntry
        if (top != null &&
            baseSegment(top.destination.route) == baseSegment(route) &&
            !top.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        ) {
            logger.d(
                tag = LogTags.NAV,
                message = "push skipped | route=$route reason=in-flight",
            )
            return
        }
        navigate(route, builder)
    }

    private fun baseSegment(route: String?): String? =
        route
            ?.substringBefore('?')
            ?.substringBefore('/')

    private fun push(screen: AppScreen) {
        val root = rootNavController ?: return
        val tabs = tabsNavController ?: return
        when (screen) {
            // ===== root-стек =====
            AppScreen.DictionarySetup ->
                root.navigateGated(RootPoint.DICTIONARY_SETUP.route) {
                    launchSingleTop = true
                    popUpTo(RootPoint.SPLASH.route) { inclusive = true }
                }

            is AppScreen.DictionaryCreate ->
                root.navigateGated(
                    screen.editId
                        ?.let { "DICTIONARY_CREATE?editId=$it" }
                        ?: "DICTIONARY_CREATE",
                ) { launchSingleTop = true }

            AppScreen.DictionaryList ->
                root.navigateGated(RootPoint.DICTIONARY_LIST.route) { launchSingleTop = true }

            AppScreen.Main -> openMainScreen(root)

            AppScreen.ExitApp -> finishApp?.invoke()

            // ===== tabs-стек =====
            is AppScreen.WordCard ->
                tabs.navigateGated(MainRoutes.wordCard(screen.wordId)) { launchSingleTop = true }

            is AppScreen.PerDictionaryComponents ->
                tabs.navigateGated(MainRoutes.perDictionaryComponents(screen.dictionaryId)) {
                    launchSingleTop = true
                }

            is AppScreen.ChatQuiz ->
                tabs.navigateGated(MainRoutes.quizChat(screen.quizType)) { launchSingleTop = true }

            AppScreen.AboutApp ->
                tabs.navigateGated(MainRoutes.ABOUT_APP) { launchSingleTop = true }

            is AppScreen.WebView ->
                tabs.navigateGated(MainRoutes.webView(screen.pageKey)) { launchSingleTop = true }
        }
    }

    /**
     * Pop снимает верхний ВИДИМЫЙ экран. Если root-стек стоит НЕ на
     * Main (список/форма словарей поверх) — видим root, попается он;
     * tabs-стек попается только когда root показывает Main. Особый
     * случай: закрытие формы первичной настройки ведёт на Main (стек
     * под ней пуст — SPLASH схлопнут при входе).
     */
    private fun pop() {
        val root = rootNavController ?: return
        val tabs = tabsNavController ?: return
        if (root.currentDestination?.route == RootPoint.DICTIONARY_SETUP.route) {
            openMainScreen(root)
            return
        }
        val rootShowsMain = root.currentDestination?.route == MainPoint.MAIN.route
        when {
            !rootShowsMain -> root.popBackStack()
            tabs.previousBackStackEntry != null -> tabs.popBackStack()
            else -> root.popBackStack()
        }
    }

    /**
     * Вход на главный экран схлопывает стек до себя: если MAIN уже в
     * back stack — pop до него, иначе navigate с popUpTo текущего.
     */
    private fun openMainScreen(root: NavHostController) {
        @Suppress("RestrictedApi")
        val isMainInBackStack =
            root.currentBackStack.value.any { it.destination.route == MainPoint.MAIN.route }
        if (isMainInBackStack) {
            root.popBackStack(MainPoint.MAIN.route, false)
        } else {
            root.navigateGated(RootPoint.MAIN_ROUTER.route) {
                root.currentDestination?.route?.let { currentRoute ->
                    launchSingleTop = true
                    popUpTo(currentRoute) { inclusive = true }
                }
            }
        }
    }
}
