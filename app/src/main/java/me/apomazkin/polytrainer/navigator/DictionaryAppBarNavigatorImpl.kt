package me.apomazkin.polytrainer.navigator

import me.apomazkin.dictionaryappbar.DictionaryAppBarNavigator

class DictionaryAppBarNavigatorImpl(
    private val onOpenDictionaryCreate: () -> Unit,
) : DictionaryAppBarNavigator {
    override fun back() {
        // shared widget — back делегируется хост-экраном
    }

    override fun openDictionaryCreate() = onOpenDictionaryCreate()
}
