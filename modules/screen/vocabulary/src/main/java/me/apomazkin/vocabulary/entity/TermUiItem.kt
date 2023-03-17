package me.apomazkin.vocabulary.entity

import java.util.*


data class TermUiItem(
    val id: Long,
    val wordValue: String,
    val langId: Long,
    val addDate: Date,
    val changeDate: Date? = null,
    val lexemeList: List<LexemeUiItem> = listOf(),
    val isExpand: Boolean = false,
    val isDropDownOpen: Boolean = false,
)

data class LexemeUiItem(
    val id: Long,
    val wordId: Long,
    val definition: String,
    val category: LexemeLabel,
)