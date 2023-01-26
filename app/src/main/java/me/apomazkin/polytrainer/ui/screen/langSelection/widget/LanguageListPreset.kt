package me.apomazkin.polytrainer.ui.screen.langSelection.widget

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.MutableState
import com.blongho.country_data.World
import me.apomazkin.polytrainer.ui.screen.langSelection.LanguageData
import me.apomazkin.polytrainer.ui.screen.langSelection.toStringName

// TODO: вынести LanguageData.langList в параметры.
// TODO: World должна вызываться во вьюмодели, иначе тут не работает превью
fun LazyListScope.languageListPreset(
    selected: MutableState<String?>
) {
    items(LanguageData.langList) { item ->
        val flagRes = World.getFlagOf(item.numericCode)
        val langName = item.toStringName()
        LanguageItemWidget(
            flagRes = flagRes,
            value = langName,
            isSelected = selected.value == langName
        ) {
            if (selected.value == it) {
                selected.value = null
            } else {
                selected.value = it
            }
        }
    }
}