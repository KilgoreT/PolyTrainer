package me.apomazkin.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.apomazkin.main.widget.BottomBarWidget

enum class TabPoint(val route: String) {
    VOCABULARY("vocabulary"),
    TRAINING("training"),
    DASHBOARD("dashboard"),
}

@Composable
fun MainScreen(
    mainUiDeps: MainUiDeps,
    onAddLang: () -> Unit,
) {
    val navController = rememberNavController()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        NavHost(
            modifier = Modifier
                .weight(1F),
            navController = navController,
            startDestination = TabPoint.VOCABULARY.route
        ) {
            vocabulary(
                navController = navController,
                mainUiDeps = mainUiDeps,
                onAddLang = onAddLang,
            )
//            composable(TabPoint.VOCABULARY.route) {
//                mainUiDeps.VocabularyTab(onAddLang = onAddLang)
//            }
            composable(TabPoint.TRAINING.route) {}
            composable(TabPoint.DASHBOARD.route) {}
        }
        BottomBarWidget(navController = navController)
    }
}

//@Composable
//@PreviewScreen
//private fun Preview() {
//    AppTheme {
//        MainScreen(
//            mainUiDeps = object : MainUiDeps {
//                @Composable
//                override fun VocabularyTab(
//                    onAddLang: () -> Unit,
//                ) {
//                }
//            },
//        ) {}
//    }
//}