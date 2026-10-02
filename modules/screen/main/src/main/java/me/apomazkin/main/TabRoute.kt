package me.apomazkin.main

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Высота нижней панели табов. Панель лежит поверх нижней части NavHost
 * (вложенный экран сразу полноразмерный), поэтому экранам табов нужен
 * отступ снизу на её высоту. Значение — замер панели в [MainScreen].
 */
internal val LocalBottomBarInset = compositionLocalOf { 80.dp }

/**
 * Горизонтальная позиция экрана каждого таба (route → x в px). Панель
 * сдвигается на позицию своего таба и едет вместе с его экраном при
 * переходе на вложенный экран и обратно.
 */
internal val LocalTabScreenOffsets =
    staticCompositionLocalOf<SnapshotStateMap<String, Float>> {
        error("LocalTabScreenOffsets не задан")
    }

/**
 * Маршрут таба: экран получает отступ снизу под панель и сообщает свою
 * горизонтальную позицию. Сами экраны табов про панель не знают.
 */
internal fun NavGraphBuilder.tabComposable(
    point: TabPoint,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(point.route) { entry ->
        val offsets = LocalTabScreenOffsets.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { offsets[point.route] = it.positionInRoot().x }
                .padding(bottom = LocalBottomBarInset.current),
        ) {
            content(entry)
        }
    }
}
