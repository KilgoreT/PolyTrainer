package me.apomazkin.main.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.apomazkin.main.R
import me.apomazkin.main.TabPoint
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

interface TabNavigator {
    fun openTab(tab: TabPoint)
}

sealed class Tabs(
    val point: TabPoint,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    data object Vocabulary : Tabs(
        point = TabPoint.VOCABULARY,
        titleRes = R.string.item_title_vocabulary,
        iconRes = R.drawable.ic_tab_vocabulary,
    )

    data object Training : Tabs(
        point = TabPoint.QUIZ,
        titleRes = R.string.item_title_training,
        iconRes = R.drawable.ic_tab_training,
    )

    data object Stats : Tabs(
        point = TabPoint.STATS,
        titleRes = R.string.item_title_stats,
        iconRes = R.drawable.ic_tab_stats,
    )

    data object Settings : Tabs(
        point = TabPoint.SETTINGS,
        titleRes = R.string.item_title_settings,
        iconRes = R.drawable.ic_tab_settings,
    )
}

/**
 * @param tabScreenOffsets горизонтальная позиция экрана каждого таба
 *   (route → x в px): панель едет вместе с экраном своего таба.
 * @param onBarMeasured высота панели в px — для отступа экранов табов.
 */
@Composable
fun BottomBarWidget(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    tabScreenOffsets: Map<String, Float> = emptyMap(),
    onBarMeasured: (Int) -> Unit = {},
) {

    val tabs: List<Tabs> = remember {
        listOf(
            Tabs.Vocabulary,
            Tabs.Training,
            Tabs.Stats,
            Tabs.Settings,
        )
    }

    val tabNavigator: TabNavigator = remember {
        object : TabNavigator {
            override fun openTab(tab: TabPoint) {
                navController.navigate(tab.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destination by remember(navBackStackEntry?.destination) {
        derivedStateOf { navBackStackEntry?.destination }
    }
    val currentRoute = destination?.route
    val tabRoutes = remember(tabs) { tabs.map { it.point.route } }
    val isTab = currentRoute != null && currentRoute in tabRoutes
    // Память маршрутов — обычные поля, не State: панель и так
    // перекомпонуется при смене маршрута, а запись State во время
    // композиции дала бы лишний проход.
    val routes = remember { RouteMemory() }
    // Последний открытый таб: панель принадлежит его экрану — едет вместе
    // с ним на вложенный экран и обратно, выделение остаётся на нём.
    if (isTab) routes.lastTab = currentRoute
    val lastTabRoute = routes.lastTab
    // Между табами панель общая и стоит на месте; на остальных переходах
    // (таб ↔ вложенный, вложенный ↔ вложенный) следует за экраном таба.
    val followTabScreen = remember(currentRoute) {
        val fromTab = routes.previous in tabRoutes
        routes.previous = currentRoute
        !(fromTab && isTab)
    }

    NavigationBar(
        modifier = modifier
            .onSizeChanged { onBarMeasured(it.height) }
            // Сдвиг читается в фазе отрисовки: панель едет кадр в кадр с
            // экраном, без перекомпозиции.
            .graphicsLayer {
                translationX = if (followTabScreen) {
                    lastTabRoute?.let { tabScreenOffsets[it] } ?: 0f
                } else {
                    0f
                }
            },
        containerColor = Color.Transparent,
    ) {
        tabs.forEach { tab ->
            key(tab.point.route) {
                BottomBarItem(
                    titleRes = tab.titleRes,
                    iconRes = tab.iconRes,
                    isSelected = lastTabRoute == tab.point.route
                ) {
                    tabNavigator.openTab(tab.point)
                }
            }
        }
    }
}

/** Маршруты, которые панель помнит между переходами. */
private class RouteMemory {
    var lastTab: String? = null
    var previous: String? = null
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            BottomBarWidget(
                navController = rememberNavController()
            )
        }
    }
}