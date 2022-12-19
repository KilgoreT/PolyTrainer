package me.apomazkin.polytrainer.navigation

data class NavEntity(
        val index: Int = 0,
        val route: String,
        val deepLink: String? = null,
)
