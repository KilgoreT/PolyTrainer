package me.apomazkin.vocabulary.ui.widget.topBar

import androidx.annotation.DrawableRes
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.entity.LangUiEntity
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.TopBarActionMsg

@Composable
fun LangDropDownWidget(
    @DrawableRes iconRes: Int,
    langList: List<LangUiEntity>,
    isExpand: Boolean,
    onChangeLang: (numericCode: Int) -> Unit,
    onAddLang: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    IconButton(
        modifier = Modifier,
        onClick = {
            sendMessage(TopBarActionMsg.ExpandLangMenu(expand = true))
        }
    ) {
        ImageFlagWidget(
            flagRes = iconRes
        )
        DropdownMenu(
            expanded = isExpand,
            onDismissRequest = {
                sendMessage(TopBarActionMsg.ExpandLangMenu(expand = false))
            }
        ) {
            langList.forEach {
                key(it) {
                    ItemLangMenuWidget(
                        iconRes = it.iconRes,
                        title = it.title
                    ) {
                        sendMessage(TopBarActionMsg.ExpandLangMenu(expand = false))
                        onChangeLang.invoke(it.numericCode)
                    }
                }
            }
            AddLangMenuItem {
                sendMessage(TopBarActionMsg.ExpandLangMenu(expand = false))
                onAddLang.invoke()
            }
        }
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        LangDropDownWidget(
            iconRes = R.drawable.example_ic_flag_gb,
            isExpand = false,
            langList = listOf(
                LangUiEntity(
                    iconRes = R.drawable.example_ic_flag_gb,
                    title = "Бритишь",
                    numericCode = 1,
                ),
                LangUiEntity(
                    iconRes = R.drawable.example_ic_flag_gb,
                    title = "Спанишь",
                    numericCode = 1,
                ),
                LangUiEntity(
                    iconRes = R.drawable.example_ic_flag_gb,
                    title = "Лягушатишь",
                    numericCode = 1,
                ),
            ),
            onChangeLang = {},
            onAddLang = {},
            sendMessage = {}
        )
    }
}