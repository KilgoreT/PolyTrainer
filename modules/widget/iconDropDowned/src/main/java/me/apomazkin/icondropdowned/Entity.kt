package me.apomazkin.icondropdowned

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector

data class DropData(
    val icon: ImageVector,
    @StringRes val contentDescription: Int = R.string.flag_content_description,
    val items: List<DropDataItem>,
)

sealed class DropDataItem(
    val id: Int = -1,
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val contentDescriptionRes: Int,
) {
    data class Delete(
        val iconDelete: ImageVector = Icons.Default.Delete,
        @StringRes val titleDelete: Int = R.string.menu_item_delete,
        @StringRes val contentDescriptionDelete: Int = titleDelete,
    ) : DropDataItem(
        icon = iconDelete,
        titleRes = titleDelete,
        contentDescriptionRes = contentDescriptionDelete
    )

    data class Edit(
        val iconEdit: ImageVector = Icons.Default.Edit,
        @StringRes val titleEdit: Int = R.string.menu_item_edit,
        @StringRes val contentDescriptionEdit: Int = titleEdit,
    ) : DropDataItem(
        icon = iconEdit,
        titleRes = titleEdit,
        contentDescriptionRes = contentDescriptionEdit
    )

    data class CustomAction(
        val customId: Int,
        val customIcon: ImageVector,
        @StringRes val customTitle: Int,
        @StringRes val contentDescription: Int = customTitle,
    ) : DropDataItem(customId, customIcon, customTitle, contentDescription)
}

val dataHelper = DropData(
    icon = Icons.Default.MoreVert,
    items = listOf(
        DropDataItem.Edit(),
        DropDataItem.Delete()
    )
)