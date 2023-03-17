package me.apomazkin.main.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.NavigationBar
import androidx.compose.runtime.*
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
    @DrawableRes val iconSelectedRes: Int,
) {
    object Vocabulary : Tabs(
        point = TabPoint.VOCABULARY,
        titleRes = R.string.item_title_vocabulary,
        iconRes = R.drawable.ic_tab_vocabulary,
        iconSelectedRes = R.drawable.ic_tab_vocabulary_selected,
    )

    object Training : Tabs(
        point = TabPoint.TRAINING,
        titleRes = R.string.item_title_training,
        iconRes = R.drawable.ic_tab_training,
        iconSelectedRes = R.drawable.ic_tab_training_selected,
    )

    object Dashboard : Tabs(
        point = TabPoint.DASHBOARD,
        titleRes = R.string.item_title_dashboard,
        iconRes = R.drawable.ic_tab_dashboard,
        iconSelectedRes = R.drawable.ic_tab_dashboard_selected,
    )
}

@Composable
fun BottomBarWidget(
    navController: NavHostController
) {

    val tabs: List<Tabs> = remember {
        listOf(
            Tabs.Vocabulary,
            Tabs.Training,
            Tabs.Dashboard,
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
    destination?.let { navDest ->
        if (navDest.route in tabs.map { it.point.route }) {
            NavigationBar {

                tabs.forEach { tab ->
                    key(tab.point.route) {
                        BottomBarItem(
                            titleRes = tab.titleRes,
                            iconRes = tab.iconRes,
                            iconSelectedRes = tab.iconSelectedRes,
                            isSelected = destination?.route == tab.point.route
                        ) {
                            tabNavigator.openTab(tab.point)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        BottomBarWidget(
            navController = rememberNavController()
        )
    }
}