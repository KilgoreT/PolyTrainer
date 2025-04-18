@file:OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class
)

package me.apomazkin.chippicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp


interface ChipValue {
    val valueRes: Int
}

sealed interface ChipPicker {
    object Off : ChipPicker
    data class Selected<T : ChipValue>(val chipValue: T) : ChipPicker
}

@Composable
fun <T : ChipValue> ChipPickerWidget(
    title: String,
    pickerValue: ChipPicker,
    chipList: List<T>,
    onChipSelect: (T) -> Unit,
    onResetChip: () -> Unit,
    editable: Boolean,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    changeIcon: ImageVector = Icons.Default.Close,
    changeIconContentDescription: String = stringResource(id = R.string.changeIconContentDescription)
) {
    /*  backUp... variable need for animation,
        when picker value is changed, but value is needed for exit animation. */
    val backUpValue = remember { mutableIntStateOf(-1) }
    val backUpChip = remember {
        object : ChipValue {
            override val valueRes: Int
                get() = backUpValue.intValue
        }
    }
    val currentChip: State<ChipValue> = remember(pickerValue) {
        derivedStateOf {
            when (pickerValue) {
                is ChipPicker.Selected<*> -> {
                    backUpValue.intValue = pickerValue.chipValue.valueRes
                    pickerValue.chipValue
                }
                is ChipPicker.Off -> backUpChip
            }
        }
    }

    Column(
        modifier = Modifier
    ) {
        Text(
            text = title,
            style = titleStyle,
        )

        Box {
            this@Column.AnimatedVisibility(
                visible = pickerValue is ChipPicker.Off,
                exit = shrinkOut() + fadeOut(),
                enter = expandIn() + fadeIn(),
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
//                    rows = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = chipList,
                        key = { it.valueRes }
                    ) { item ->
                        SuggestionChip(
                            onClick = { onChipSelect(item) },
                            label = {
                                Text(
                                    text = stringResource(id = item.valueRes),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
//                            border = SuggestionChipDefaults.suggestionChipBorder(
//                                borderColor = MaterialTheme.colorScheme.outline
//                            ),
                        )
                    }
                }
            }

            this@Column.AnimatedVisibility(
                visible = pickerValue is ChipPicker.Selected<*>,
                exit = fadeOut(),
                enter = fadeIn(),
            ) {
                InputChip(
                    selected = true,
                    enabled = editable,
                    onClick = onResetChip,
                    label = {
                        Text(
                            text = stringResource(id = currentChip.value.valueRes),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (editable) {
                            Icon(
                                imageVector = changeIcon,
                                contentDescription = changeIconContentDescription
                            )
                        }
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        disabledSelectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                )
            }
        }
    }
}