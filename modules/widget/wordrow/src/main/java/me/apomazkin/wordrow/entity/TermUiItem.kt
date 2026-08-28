package me.apomazkin.wordrow.entity

import java.util.Date


data class TermUiItem(
    val id: Long,
    val wordValue: String,
    val dictionaryId: Long,
    val addDate: Date,
    val changeDate: Date? = null,
    val lexemeList: List<LexemeUiItem> = listOf(),
    // Поля selection/expand — семантика wordstab (IS493 D8.1/F-3):
    // переезжают вместе с entity; read-only потребители (groupstab)
    // используют дефолты, рамка/раскрытие не рисуются.
    val isExpand: Boolean = false,
    val isSelected: Boolean = false,
)
