package me.apomazkin.testsheets.builder

import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties

class SheetDoc private constructor(
    private val title: String,
    private val sheets: List<SheetPage>,
) {

    fun get(): Spreadsheet = Spreadsheet().also { spreadSheet ->
        val spreadsheetProperties = SpreadsheetProperties()
        spreadsheetProperties.title = title
        spreadSheet.properties = spreadsheetProperties
        spreadSheet.sheets = sheets.map { it.get() }
    }

    data class Builder(
        var title: String = "",
        var sheets: MutableList<SheetPage> = mutableListOf(),
    ) {
        fun title(title: String) = apply { this.title = title }
        fun addSheetPage(sheetPage: SheetPage) = apply { this.sheets.add(sheetPage) }
        fun build() = SheetDoc(title, sheets.toList())
    }
}