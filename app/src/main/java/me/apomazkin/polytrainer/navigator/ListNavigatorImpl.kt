package me.apomazkin.polytrainer.navigator

import androidx.navigation.NavController
import me.apomazkin.dictionary.list.ListNavigator

class ListNavigatorImpl(
    private val navController: NavController,
    private val onExit: () -> Unit,
) : ListNavigator {
    override fun back() {
        navController.popBackStack()
    }

    override fun exit() = onExit()

    override fun openEdit(id: Long) {
        navController.navigate("DICTIONARY_CREATE?editId=$id") {
            launchSingleTop = true
        }
    }

    override fun openCreate() {
        navController.navigate("DICTIONARY_CREATE") {
            launchSingleTop = true
        }
    }
}
