package me.apomazkin.vocabulary.logic

import me.apomazkin.vocabulary.entity.TermUiItem
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

class DataHelper {
    companion object {
        private val localDate: LocalDate = LocalDate.now()
        val termList = listOf(
            TermUiItem(
                id = 1,
                langId = 0,
                wordValue = "1",
                isSelected = false,
                addDate = Date.from(
                    localDate
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                ),
            ),
            TermUiItem(
                id = 2,
                langId = 0,
                wordValue = "2",
                isSelected = false,
                addDate = Date.from(
                    localDate
                        .minusDays(1)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                ),
            ),
            TermUiItem(
                id = 3,
                langId = 0,
                wordValue = "3",
                isSelected = false,
                addDate = Date.from(
                    localDate
                        .minusDays(2)
                        .atStartOfDay()
                        .toInstant(ZoneOffset.UTC)
                ),
            ),
        )
    }
}