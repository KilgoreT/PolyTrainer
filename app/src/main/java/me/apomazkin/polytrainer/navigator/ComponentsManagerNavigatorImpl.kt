package me.apomazkin.polytrainer.navigator

import me.apomazkin.components_manager.ComponentsManagerNavigator

class ComponentsManagerNavigatorImpl(
    private val onBack: () -> Unit,
) : ComponentsManagerNavigator {
    override fun back() = onBack()
}
