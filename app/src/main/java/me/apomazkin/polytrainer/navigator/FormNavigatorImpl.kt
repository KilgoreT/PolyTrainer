package me.apomazkin.polytrainer.navigator

import me.apomazkin.dictionary.form.FormNavigator

class FormNavigatorImpl(
    private val onBack: () -> Unit,
) : FormNavigator {
    override fun back() = onBack()
}
