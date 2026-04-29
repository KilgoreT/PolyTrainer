package me.apomazkin.dictionary.model

import androidx.compose.runtime.Immutable

@Immutable
data class DictionaryListItem(
    val id: Long,
    val name: String,
    val flagRes: Int? = null,
)
