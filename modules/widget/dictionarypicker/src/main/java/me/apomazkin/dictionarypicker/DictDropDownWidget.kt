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
        openDictionaryCreate: () -> Unit,
        onOpenDropDown: () -> Unit,
        onDismiss: () -> Unit,
        onItemClick: (dict: DictUiEntity) -> Unit,
) {
    IconDropdownWidget(
        isDropDownOpen = isExpand,
        onClickDropDown = onOpenDropDown,
        onDismissRequest = onDismiss,
        icon = {
            val flagRes = currentDict?.flagRes ?: 0
            if (flagRes != 0) {
                ImageFlagWidget(flagRes = flagRes)
            }
        },
    ) {
        dictList.forEach {
            key(it) {
                ItemDictMenuWidget(
                    iconRes = it.flagRes,
                    title = it.title,
                    isSelected = it.id == currentDict?.id,
                ) {
                    onItemClick.invoke(it)
                }
            }
        }
        DividerMenuItem()
        AddDictMenuWidget {
            onDismiss.invoke()
            openDictionaryCreate.invoke()
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
                                id = 1L,
                                flagRes = R.drawable.example_ic_flag_gb,
                                title = "Бритишь",
                                numericCode = 2,
                        ),
                        DictUiEntity(
                                id = 2L,
                                flagRes = R.drawable.example_ic_flag_gb,
                                title = "Спанишь",
                                numericCode = 1,
                        ),
                        DictUiEntity(
                                id = 3L,
                                flagRes = R.drawable.example_ic_flag_gb,
                                title = "Лягушатишь",
                                numericCode = 3,
                        ),
                ),
                currentDict = DictUiEntity(
                    id = 2L,
                    flagRes = R.drawable.example_ic_flag_gb,
                    title = "Спанишь",
                    numericCode = 1,
                ),
                isExpand = false,
                openDictionaryCreate = {},
                onOpenDropDown = {},
                onDismiss = {},
                onItemClick = {}
        )
    }
}