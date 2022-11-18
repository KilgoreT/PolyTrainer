package me.apomazkin.feature_vocabulary_impl.ui.wordList.builder

import com.google.api.services.sheets.v4.model.RowData

class RowLine private constructor(
    private val cellData: List<CellSeal>,
) {

    fun get() = RowData().also { rowData ->
        rowData.setValues(cellData.map { it.get() })
    }

    data class Builder(
        var cellData: MutableList<CellSeal> = mutableListOf(),
    ) {
        fun addCell(row: CellSeal) = apply { this.cellData.add(row) }
        fun build() = RowLine(cellData)
    }
}