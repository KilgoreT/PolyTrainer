package me.apomazkin.testsheets.builder

import com.google.api.services.sheets.v4.model.GridData
import com.google.api.services.sheets.v4.model.Sheet
import com.google.api.services.sheets.v4.model.SheetProperties
import me.apomazkin.feature_vocabulary_impl.ui.wordList.builder.RowLine

class SheetPage private constructor(
    private val title: String,
    private val lines: List<RowLine>,
) {

    fun get() = Sheet().also { sheet ->
        val sheetProperties = SheetProperties()
        sheetProperties.title = title
        sheet.properties = sheetProperties
        val grid = GridData()
        grid.rowData = lines.map { it.get() }
        sheet.data = listOf(grid)
    }

    data class Builder(
        var title: String = "",
        var lines: MutableList<RowLine> = mutableListOf(),

        ) {
        fun title(title: String) = apply { this.title = title }
        fun addLine(line: RowLine) = apply { this.lines.add(line) }
        fun build() = SheetPage(title, lines)
    }
}