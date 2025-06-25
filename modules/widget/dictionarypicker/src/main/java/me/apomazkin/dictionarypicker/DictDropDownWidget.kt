package me.apomazkin.dictionarypicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarypicker.items.AddDictMenuWidget
import me.apomazkin.dictionarypicker.items.ItemDictMenuWidget
import me.apomazkin.dictionarypicker.state.LangPickerState
import me.apomazkin.icondropdowned.IconDropdownWidget
import me.apomazkin.icondropdowned.DividerMenuItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictDropDownWidget(
    state: LangPickerState,
    isExpand: Boolean,
    openAddDict: () -> Unit,
    onOpenDropDown: () -> Unit,
    onDismiss: () -> Unit,
    onItemClick: (dict: DictUiEntity) -> Unit,
) {
    IconDropdownWidget(
        isDropDownOpen = isExpand,
        onClickDropDown = onOpenDropDown,
        onDismissRequest = onDismiss,
        icon = { ImageFlagWidget(flagRes = state.currentDict.flagRes) },
    ) {
        state.availableDictList.forEach {
            key(it) {
                ItemDictMenuWidget(
                    iconRes = it.flagRes,
                    title = it.title,
                    isSelected = it.numericCode == state.currentDict.numericCode,
                ) {
                    onItemClick.invoke(it)
                }
            }
        }
        DividerMenuItem()
        AddDictMenuWidget {
            onDismiss.invoke()
            openAddDict.invoke()
        }
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        DictDropDownWidget(
            state = LangPickerState(
                isLoading = false,
                currentDict = DictUiEntity(
                    flagRes = R.drawable.example_ic_flag_gb,
                    title = "Спанишь",
                    numericCode = 1,
                ),
                availableDictList = listOf(
                    DictUiEntity(
                        flagRes = R.drawable.example_ic_flag_gb,
                        title = "Бритишь",
                        numericCode = 2,
                    ),
                    DictUiEntity(
                        flagRes = R.drawable.example_ic_flag_gb,
                        title = "Спанишь",
                        numericCode = 1,
                    ),
                    DictUiEntity(
                        flagRes = R.drawable.example_ic_flag_gb,
                        title = "Лягушатишь",
                        numericCode = 3,
                    ),
                ),
                isDropDownMenuOpen = false,
            ),
            isExpand = false,
            openAddDict = {},
            onOpenDropDown = {},
            onDismiss = {},
            onItemClick = {}
        )
    }
}