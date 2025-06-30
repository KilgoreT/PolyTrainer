package me.apomazkin.dictionarypicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.dictionarypicker.items.AddDictMenuWidget
import me.apomazkin.dictionarypicker.items.ItemDictMenuWidget
import me.apomazkin.icondropdowned.IconDropdownWidget
import me.apomazkin.icondropdowned.DividerMenuItem
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictDropDownWidget(
        dictList: List<DictUiEntity>,
        currentDict: DictUiEntity?,
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
        icon = { ImageFlagWidget(flagRes = currentDict?.flagRes ?: 0) },
    ) {
        dictList.forEach {
            key(it) {
                ItemDictMenuWidget(
                    iconRes = it.flagRes,
                    title = it.title,
                    isSelected = it.numericCode == currentDict?.numericCode,
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
                dictList = listOf(
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
                currentDict = DictUiEntity(
                    flagRes = R.drawable.example_ic_flag_gb,
                    title = "Спанишь",
                    numericCode = 1,
                ),
                isExpand = false,
                openAddDict = {},
                onOpenDropDown = {},
                onDismiss = {},
                onItemClick = {}
        )
    }
}