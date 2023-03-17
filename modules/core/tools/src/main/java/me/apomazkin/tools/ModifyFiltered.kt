package me.apomazkin.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

fun <T> List<T>.insertToBegin(item: T): List<T> {
    return this.toMutableList().apply {
        add(0, item)
    }.toList()
}

fun <T> List<T>.getFilteredOrFirst(
    predicate: (T) -> Boolean,
): T? {
    return this.firstOrNull { predicate.invoke(it) }
        ?: this.firstOrNull()
}

fun <T> List<T>.modifyFilteredOrFirst(
    predicate: (T) -> Boolean,
    action: (T) -> T,
): List<T> {
    return if (this.any { predicate.invoke(it) }) {
        this.modifyFiltered(predicate, action)
    } else {
        this.modifyFirst(action)
    }
}

fun <T> List<T>.modifyFirst(action: (T) -> T): List<T> {
    return this.mapIndexed { index, item ->
        if (index == 0) action(item)
        else item
    }
}

fun <T> List<T>.modifyFiltered(predicate: (T) -> Boolean, action: (T) -> T): List<T> {
    return this.map { if (predicate(it)) action(it) else it }
}

fun <T> StateFlow<List<T>>.modifyFiltered(
    predicate: (T) -> Boolean,
    action: (T) -> T,
    scope: CoroutineScope,
): StateFlow<List<T>> {
    return this.map { it.modifyFiltered(predicate, action) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
}