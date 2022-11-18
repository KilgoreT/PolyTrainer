package me.apomazkin.feature_vocabulary_impl.ui.wordList.builder

import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.ExtendedValue
import java.util.*

sealed interface CellSeal {
    fun get(): CellData

    class CellString(
        private val value: String?
    ) : CellSeal {
        override fun get() = CellData().also { cellData ->
            val extendedValue = ExtendedValue()
            value?.let { extendedValue.stringValue = it }
            cellData.userEnteredValue = extendedValue
        }
    }

    class CellLong(
        private val value: Long?
    ) : CellSeal {
        override fun get() = CellData().also { cellData ->
            val extendedValue = ExtendedValue()
            value?.let { extendedValue.numberValue = it.toDouble() }
            cellData.userEnteredValue = extendedValue
        }
    }

    class CellDate(
        private val value: Date?
    ) : CellSeal {
        override fun get() = CellData().also { cellData ->
            val extendedValue = ExtendedValue()
            value?.let { extendedValue.numberValue = it.time.toDouble() }
            cellData.userEnteredValue = extendedValue
        }
    }
}