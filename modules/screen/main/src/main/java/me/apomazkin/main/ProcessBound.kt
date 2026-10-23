package me.apomazkin.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavHostController
import java.util.UUID

/**
 * Метка процесса: случайная строка, своя у каждого запущенного процесса. Её
 * сохраняет [ProcessBound]: после гибели процесса восстановленная метка уже не
 * равна текущей.
 */
internal object ProcessStamp {
    val value: String = UUID.randomUUID().toString()
}

/**
 * Экран, который не переживает гибель процесса (AWP-27, WCL-14). Метка процесса
 * при показе сохраняется вместе с записью стека навигации; система убила
 * процесс — стек восстановился, а метка в нём чужая: содержимое не строится, а
 * сам экран снимается со стека, приложение остаётся на вкладке под ним. Поворот
 * экрана процесс не убивает, метка та же — экран на месте.
 */
@Composable
internal fun ProcessBound(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val savedStamp = rememberSaveable { ProcessStamp.value }
    if (savedStamp == ProcessStamp.value) {
        content()
    } else {
        LaunchedEffect(Unit) { navController.popBackStack() }
    }
}
