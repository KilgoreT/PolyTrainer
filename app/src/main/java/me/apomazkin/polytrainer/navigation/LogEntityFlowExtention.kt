@file:OptIn(ExperimentalCoroutinesApi::class)

package me.apomazkin.polytrainer.navigation

import kotlinx.coroutines.ExperimentalCoroutinesApi

//fun NavHostController.toLogEntityFlow(): Flow<NavLogEntity> {
//    return this.currentBackStackEntryFlow.mapLatest {
//        val intent = it.arguments?.get(NavController.KEY_DEEP_LINK_INTENT) as? Intent
//        val currentRoute = it.destination.route ?: Constants.UNKNOWN_POINT
//        val stackList = this.backQueue.reversed().mapIndexed { index, navBackStackEntry ->
//            NavEntity(
//                index = index,
//                route = navBackStackEntry.destination.route ?: Constants.ROOT_POINT,
//                deepLink = intent?.data.toString()
//            )
//        }
//        NavLogEntity(
//            current = NavEntity(route = currentRoute),
//            stack = stackList,
//        )
//    }
//}