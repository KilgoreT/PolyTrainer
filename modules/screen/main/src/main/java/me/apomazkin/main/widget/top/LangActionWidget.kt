package me.apomazkin.main.widget.top

import androidx.annotation.DrawableRes
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import me.apomazkin.main.R
import me.apomazkin.main.entity.LangUiEntity
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.ImageFlagWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun LangActionWidget(
    @DrawableRes iconRes: Int,
    langList: List<LangUiEntity>,
    onChangeLang: (numericCode: Int) -> Unit,
    onAddLang: () -> Unit,
) {
    val isDropdownMenuExpand = remember { mutableStateOf(false) }

    IconButton(
        modifier = Modifier,
        onClick = {
            isDropdownMenuExpand.value = true
        }
    ) {
        ImageFlagWidget(
            flagRes = iconRes
        )
        DropdownMenu(
            expanded = isDropdownMenuExpand.value,
            onDismissRequest = {
                isDropdownMenuExpand.value = false
            }
        ) {
            langList.forEach {
                key(it.iconRes) {
                    ItemLangMenuItem(
                        iconRes = it.iconRes,
                        title = it.title
                    ) {
                        isDropdownMenuExpand.value = false
                        onChangeLang.invoke(it.numericCode)
                    }
                }
            }
            AddLangMenuItem { onAddLang.invoke() }
        }
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        LangActionWidget(
            iconRes = R.drawable.example_ic_flag_gb,
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
        )
    }
}