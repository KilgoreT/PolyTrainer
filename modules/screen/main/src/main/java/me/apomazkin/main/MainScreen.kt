package me.apomazkin.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import me.apomazkin.main.widget.BottomBarWidget
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget

enum class TabPoint(val route: String) {
    VOCABULARY("vocabulary"),
    QUIZ("quiz"),
    STATS("statistic"),
    SETTINGS("settings"),
}

@Composable
fun MainScreen(
    navController: NavHostController,
    compositionRoot: CompositionRoot,
) {
    SystemBarsWidget(
        color = whiteColor,
    )
    // Высота панели — последняя ненулевая, позиции экранов табов — для
    // сдвига панели вместе с экраном своего таба.
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(80.dp) }
    val tabOffsets = remember { mutableStateMapOf<String, Float>() }
    // NavHost на всю высоту, панель — поверх его низа: вложенный экран
    // сразу полноразмерный, а панель уезжает вбок вместе с экраном таба.
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CompositionLocalProvider(
            LocalBottomBarInset provides barHeight,
            LocalTabScreenOffsets provides tabOffsets,
        ) {
            NavHost(
                modifier = Modifier
                    .fillMaxSize(),
                navController = navController,
                startDestination = TabPoint.VOCABULARY.route,
                enterTransition = { slideIn() },
                exitTransition = { slideOut() },
                popEnterTransition = { slideIn(pop = true) },
                popExitTransition = { slideOut(pop = true) },
            ) {
                vocabulary(compositionRoot = compositionRoot)
                quiz(compositionRoot = compositionRoot)
                statistic(compositionRoot = compositionRoot)
                settings(
                    navController = navController,
                    compositionRoot = compositionRoot,
                )
            }
        }
        BottomBarWidget(
            navController = navController,
            modifier = Modifier.align(Alignment.BottomCenter),
            tabScreenOffsets = tabOffsets,
            onBarMeasured = { heightPx ->
                if (heightPx > 0) barHeight = with(density) { heightPx.toDp() }
            },
        )
    }
}

/**
 * Темп переходов экранов: 400 мс, EaseInOutCubic — кривая как у движения
 * ленты квиз-чата. Нижняя панель едет вместе с экраном своего таба.
 * Тот же темп у сдвига главного экрана и «Всех словарей» в root-графе.
 */
const val SCREEN_TRANSITION_DURATION_MS = 400
private val transitionSpec: FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = SCREEN_TRANSITION_DURATION_MS, easing = EaseInOutCubic)

private fun NavBackStackEntry.tabIndex(): Int =
    TabPoint.entries.indexOfFirst { it.route == destination.route }

/**
 * Куда едет переход: между табами — по их порядку в нижней панели
 * (таб правее въезжает справа, левее — слева); вглубь (карточка слова,
 * квиз-чат, «О приложении», WebView…) — справа, назад ([pop]) — влево.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.direction(
    pop: Boolean,
): SlideDirection {
    val from = initialState.tabIndex()
    val to = targetState.tabIndex()
    val forward = if (from >= 0 && to >= 0) to > from else !pop
    return if (forward) SlideDirection.Left else SlideDirection.Right
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideIn(
    pop: Boolean = false,
): EnterTransition =
    slideIntoContainer(towards = direction(pop), animationSpec = transitionSpec)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideOut(
    pop: Boolean = false,
): ExitTransition =
    slideOutOfContainer(towards = direction(pop), animationSpec = transitionSpec)
