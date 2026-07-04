package me.apomazkin.polytrainer.navigator

import me.apomazkin.per_dictionary_components.PerDictionaryComponentsNavigator

class PerDictionaryComponentsNavigatorImpl(
    private val onBack: () -> Unit,
) : PerDictionaryComponentsNavigator {
    override fun back() = onBack()
}
