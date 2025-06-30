package me.apomazkin.dictionaryappbar.mate

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionarypicker.entity.DictUiEntity

/**
 * State
 */
@Immutable
data class DictionaryAppBarState(
        val isLoading: Boolean = true,
        val currentDict: DictUiEntity? = null,
        val availableDictList: List<DictUiEntity> = emptyList(),
        val isDropDownMenuOpen: Boolean = false,
)

fun DictionaryAppBarState.showLoading() = this.copy(
        isLoading = true,
)

fun DictionaryAppBarState.hideLoading() = this.copy(
        isLoading = false,
)

fun DictionaryAppBarState.availableDictList(list: List<DictUiEntity>) = this.copy(
        availableDictList = list,
)

fun DictionaryAppBarState.currentDict(current: DictUiEntity) = this.copy(
        currentDict = current,
)

fun DictionaryAppBarState.dictMenuOn() = this.copy(
                isDropDownMenuOpen = true,
)

fun DictionaryAppBarState.dictMenuOff() = this.copy(
                isDropDownMenuOpen = false,
)
