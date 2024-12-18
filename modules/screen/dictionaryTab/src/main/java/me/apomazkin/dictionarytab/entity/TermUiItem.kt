package me.apomazkin.dictionarytab.entity

import java.util.Date


data class TermUiItem(
    val id: Long,
    val wordValue: String,
    val langId: Long,
    val addDate: Date,
    val changeDate: Date? = null,
    val lexemeList: List<LexemeUiItem> = listOf(),
    val isExpand: Boolean = false,
    val isSelected: Boolean = false,
)