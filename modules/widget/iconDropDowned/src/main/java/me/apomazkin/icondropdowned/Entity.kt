package me.apomazkin.icondropdowned

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

val DeleteIcon = IconSource.fromVector(
        imageVector = Icons.Default.Delete,
        contentDescription = StringSource
                .fromRes(resId = R.string.menu_item_delete)
)

val EditIcon = IconSource.fromVector(
        imageVector = Icons.Default.Edit,
        contentDescription = StringSource
                .fromRes(resId = R.string.menu_item_edit)
)