package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import me.apomazkin.icondropdowned.IconDropdownMenuWidget
import me.apomazkin.icondropdowned.MenuDivider
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.entity.DictUiEntity
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.TopBarActionMsg

@Composable
fun DictDropDownWidget(
    @DrawableRes iconRes: Int,
    currentDictCode: Int?,
    dictList: List<DictUiEntity>,
    isExpand: Boolean,
    openAddDict: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    IconDropdownMenuWidget(
        isDropDownOpen = isExpand,
        onClickDropDown = { sendMessage(TopBarActionMsg.ExpandDictMenu(expand = true)) },
        onDismissRequest = { sendMessage(TopBarActionMsg.ExpandDictMenu(expand = false)) },
        icon = { ImageFlagWidget(flagRes = iconRes) },
    ) {
        dictList.forEach {
            key(it) {
                ItemDictMenuWidget(
                    iconRes = it.flagRes,
                    title = it.title,
                    isSelected = it.numericCode == currentDictCode,
                ) { sendMessage(TopBarActionMsg.ChangeDict(numericCode = it.numericCode)) }
            }
        }
        MenuDivider()
        AddDictMenuItem {
            sendMessage(TopBarActionMsg.ExpandDictMenu(expand = false))
            openAddDict.invoke()
        }
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        DictDropDownWidget(
            iconRes = R.drawable.example_ic_flag_gb,
            currentDictCode = 1,
            isExpand = false,
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
            openAddDict = {},
            sendMessage = {}
        )
    }
}