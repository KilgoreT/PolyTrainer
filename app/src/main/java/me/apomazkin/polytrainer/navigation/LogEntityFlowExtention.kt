@file:OptIn(ExperimentalCoroutinesApi::class)

package me.apomazkin.polytrainer.navigation

import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

fun NavHostController.toLogEntityFlow(): Flow<NavLogEntity> {
    return this.currentBackStackEntryFlow.mapLatest {
        val intent = it.arguments?.get(NavController.KEY_DEEP_LINK_INTENT) as? Intent
        val currentRoute = it.destination.route ?: Constants.UNKNOWN_POINT
        val stackList = this.backQueue.reversed().mapIndexed { index, navBackStackEntry ->
            NavEntity(
                index = index,
                route = navBackStackEntry.destination.route ?: Constants.ROOT_POINT,
                deepLink = intent?.data.toString()
            )
        }
        NavLogEntity(
            current = NavEntity(route = currentRoute),
            stack = stackList,
        )
    }
}